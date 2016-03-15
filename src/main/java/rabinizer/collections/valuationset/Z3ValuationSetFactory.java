/*
 * Copyright (C) 2016  (See AUTHORS)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package rabinizer.collections.valuationset;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.math.IntMath;
import com.microsoft.z3.*;
import org.jetbrains.annotations.NotNull;
import rabinizer.ltl.*;

import java.util.*;
import java.util.stream.Collectors;

public class Z3ValuationSetFactory implements ValuationSetFactory {

    public final BoolExpr TRUE;
    public final BoolExpr FALSE;
    protected final BiMap<Literal, BoolExpr> mapping;
    private final Z3Visitor visitor;
    private final Context ctx;
    final Set<String> alphabet;

    public Z3ValuationSetFactory(Set<String> alphabet) {
        visitor = new Z3Visitor();
        ctx = new Context();
        TRUE = ctx.mkTrue();
        FALSE = ctx.mkFalse();

        Builder<Literal, BoolExpr> builder = new Builder<>();
        this.alphabet = ImmutableSet.copyOf(alphabet);

        for (String letter : alphabet) {
            BoolExpr pos = ctx.mkBoolConst(letter);
            BoolExpr neg = ctx.mkNot(pos);
            builder.put(new Literal(letter, false), pos);
            builder.put(new Literal(letter, true), neg);
        }

        mapping = builder.build();
    }

    @Override
    public Set<String> getAlphabet() {
        return alphabet;
    }

    @Override
    public Z3ValuationSet createEmptyValuationSet() {
        return new Z3ValuationSet(FALSE);
    }

    @Override
    public Z3ValuationSet createUniverseValuationSet() {
        return new Z3ValuationSet(TRUE);
    }

    @Override
    public Z3ValuationSet createValuationSet(Set<String> valuation) {
        return createValuationSet(valuation, alphabet);
    }

    @Override
    public Z3ValuationSet createValuationSetSet(Set<Set<String>> set) {
        Z3ValuationSet vs = createEmptyValuationSet();
        vs.addAll(set);
        return vs;
    }

    @Override
    public Z3ValuationSet createValuationSet(Set<String> valuation, Set<String> base) {
        Formula f = new Conjunction(base.stream().map(s -> new Literal(s, !valuation.contains(s))));
        return new Z3ValuationSet(createZ3(f));
    }

    @Override
    public Set<ValuationSet> createAllValuationSets() {
        return Sets.powerSet(alphabet).stream().map(this::createValuationSet).collect(Collectors.toSet());
    }

    protected Formula createRepresentative(BoolExpr expression) {
        if (expression.isTrue()) {
            return BooleanConstant.TRUE;
        } else if (expression.isFalse()) {
            return BooleanConstant.FALSE;
        } else if (expression.isAnd()) {
            Expr[] exp = expression.getArgs();
            return new Conjunction(Arrays.asList(exp).stream().map(s -> createRepresentative((BoolExpr) s)));
        } else if (expression.isOr()) {
            Expr[] exp = expression.getArgs();
            return new Disjunction(Arrays.asList(exp).stream().map(s -> createRepresentative((BoolExpr) s)));
        } else if (expression.isNot()) {
            return createRepresentative((BoolExpr) expression.getArgs()[0]).not();
        } else if (expression.isConst()) {
            return mapping.inverse().get(expression);
        } else {
            throw new IllegalArgumentException("The expression does not correspond to a Formula");
        }
    }

    protected BoolExpr createZ3(Formula f) {
        return f.accept(visitor);
    }

    protected Set<Literal> getSatAssignment(BoolExpr expression) {
        Set<Literal> result = new HashSet<>();
        Solver s = ctx.mkSolver();
        s.add(expression);
        if (s.check() != Status.SATISFIABLE) {
            throw new NoSuchElementException();
        }
        Model m = s.getModel();
        m.getConstDecls();
        for (BoolExpr e : getPropositionsOutOfBoolExpr(expression)) {
            if (m.getConstInterp(e).isTrue()) {
                result.add(mapping.inverse().get(e));
            } else if (m.getConstInterp(e).isFalse()) {
                result.add(mapping.inverse().get(ctx.mkNot(e)));
            }
        }
        return result;
    }

    protected static Set<BoolExpr> getPropositionsOutOfBoolExpr(BoolExpr bool) {
        Set<BoolExpr> result = new HashSet<>();

        if (bool.isConst()) {
            if (!bool.isTrue() && !bool.isFalse()) {
                result.add(bool);
            }
        } else {
            for (Expr exp : bool.getArgs()) {
                BoolExpr b = (BoolExpr) exp;
                result.addAll(getPropositionsOutOfBoolExpr(b));
            }
        }

        return result;
    }

    private boolean testUnsatisfiability(BoolExpr b) {
        Solver s = ctx.mkSolver();
        s.add(b);

        switch (s.check()) {
            case UNSATISFIABLE:
                return true;

            case SATISFIABLE:
                return false;

            default:
                throw new IllegalStateException();
        }
    }

    public class Z3ValuationSet extends AbstractSet<Set<String>> implements ValuationSet {
        private BoolExpr valuation;

        public Z3ValuationSet(BoolExpr val) {
            valuation = val;
        }

        @Override
        public boolean add(Set<String> e) {
            Z3ValuationSet vs = createValuationSet(e);
            return update(ctx.mkOr(valuation, vs.valuation));
        }

        @Override
        public boolean addAll(@NotNull Collection<? extends Set<String>> c) {
            if (c instanceof Z3ValuationSet) {
                return update(ctx.mkOr(((Z3ValuationSet) c).valuation, valuation));
            }

            return super.addAll(c);
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Set)) {
                return false;
            }

            Set<String> val = (Set<String>) o;
            Z3ValuationSet set = createValuationSet(val);

            return testUnsatisfiability(ctx.mkNot(ctx.mkImplies(set.valuation, valuation)));
        }

        @Override
        public boolean containsAll(@NotNull Collection<?> c) {
            return c.stream().allMatch(this::contains);
        }

        @Override
        public boolean isEmpty() {
            return testUnsatisfiability(valuation);
        }

        @Override
        public @NotNull Iterator<Set<String>> iterator() {
            return Sets.powerSet(alphabet).stream().filter(this::contains).iterator();
        }

        @Override
        public boolean remove(Object o) {
            if (o instanceof Set) {
                Z3ValuationSet vs = createValuationSet((Set<String>) o);
                return update(ctx.mkAnd(valuation, ctx.mkNot(vs.valuation)));
            }

            return false;
        }

        @Override
        public boolean removeAll(@NotNull Collection<?> c) {
            return c.stream().anyMatch(this::remove);
        }

        @Override
        public boolean retainAll(@NotNull Collection<?> c) {

            if (c instanceof Z3ValuationSet) {
                BoolExpr otherValuations = ((Z3ValuationSet) c).valuation;
                BoolExpr newValuations = ctx.mkAnd(valuation, otherValuations);
                return update(newValuations);
            }

            return super.retainAll(c);

        }

        @Override
        public ValuationSet complement() {
            return new Z3ValuationSet(ctx.mkNot(valuation));
        }

        @Override
        public boolean isUniverse() {
            return testUnsatisfiability(ctx.mkNot(valuation));
        }

        @Override
        public boolean restrictWith(Literal literal) {
            return update(ctx.mkAnd(valuation, createZ3(literal)));
        }

        @Override
        public Set<String> pickAny() {
            Set<Literal> satAssignment = getSatAssignment(valuation);
            return satAssignment.stream().filter(literal -> !literal.negated).map(literal -> literal.atom)
                    .collect(Collectors.toSet());
        }

        @Override
        public Formula toFormula() {
            return createRepresentative(valuation);
        }

        @Override
        public Z3ValuationSet clone() {
            return new Z3ValuationSet(valuation);
        }

        private boolean update(BoolExpr or) {
            if (!testUnsatisfiability(ctx.mkNot(ctx.mkEq(valuation, or)))) {
                valuation = or;
                return true;
            }

            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Z3ValuationSet) {
                BoolExpr otherZ3 = ((Z3ValuationSet) o).valuation;
                return testUnsatisfiability(ctx.mkNot(ctx.mkEq(otherZ3, valuation)));
            }

            return false;
        }

        /**
         * Over approximation
         */
        @Override
        public int size() {
            return IntMath.pow(2, this.toFormula().getAtoms().size());
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(valuation);
        }

    }

    private class Z3Visitor implements Visitor<BoolExpr> {

        @Override
        public BoolExpr visit(@NotNull BooleanConstant b) {
            return b.value ? TRUE : FALSE;
        }

        @Override
        public BoolExpr visit(@NotNull Conjunction c) {
            if (c.children.contains(BooleanConstant.FALSE)) {
                return FALSE;
            }

            BoolExpr[] b = new BoolExpr[c.children.size()];
            int index = 0;
            for (Formula elem : c.children) {
                b[index++] = elem.accept(this);
            }
            return ctx.mkAnd(b);
        }

        @Override
        public BoolExpr visit(@NotNull Disjunction d) {
            if (d.children.contains(BooleanConstant.TRUE)) {
                return TRUE;
            }

            BoolExpr[] b = new BoolExpr[d.children.size()];

            int index = 0;
            for (Formula elem : d.children) {
                b[index++] = elem.accept(this);
            }
            return ctx.mkOr(b);
        }

        @Override
        public BoolExpr visit(@NotNull Literal l) {
            return mapping.get(l);
        }

        @Override
        public BoolExpr defaultAction(@NotNull Formula f) {
            throw new UnsupportedOperationException();
        }
    }
}