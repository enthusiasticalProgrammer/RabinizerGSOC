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

package rabinizer.ltl.equivalence;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Solver;
import rabinizer.ltl.*;
import rabinizer.ltl.simplifier.Simplifier;
import rabinizer.ltl.simplifier.Simplifier.Strategy;

import java.util.*;
import java.util.function.Function;

public class Z3EquivalenceClassFactory implements EquivalenceClassFactory {

    final BoolExpr TRUE;
    final BoolExpr FALSE;
    final BiMap<Formula, BoolExpr> mapping;
    final Z3Visitor visitor;
    final Context ctx;
    final Collection<Z3EquivalenceClass> alreadyUsed;

    public Z3EquivalenceClassFactory(Formula formula) {
        Set<Formula> propositions = PropositionVisitor.extractPropositions(formula).keySet();

        visitor = new Z3Visitor();
        ctx = new Context();
        TRUE = ctx.mkTrue();
        FALSE = ctx.mkFalse();

        mapping = HashBiMap.create(propositions.size());
        alreadyUsed = new ArrayList<>(propositions.size());

        for (Formula proposition : propositions) {
            mapping.put(proposition, mkConst(proposition.toString()));
        }
    }

    @Override
    public EquivalenceClass getTrue() {
        return createEquivalenceClass(BooleanConstant.TRUE);
    }

    @Override
    public EquivalenceClass getFalse() {
        return createEquivalenceClass(BooleanConstant.FALSE);
    }

    @Override
    public EquivalenceClass createEquivalenceClass(Formula formula, Function<Formula, Optional<Boolean>> environment) {
        return probe(new Z3EquivalenceClass(formula, createZ3(formula, environment)));
    }

    @Override
    public EquivalenceClass createEquivalenceClass(Formula formula) {
        return probe(new Z3EquivalenceClass(formula, createZ3(formula, null)));
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

    protected BoolExpr createZ3(Formula f, Function<Formula, Optional<Boolean>> environment) {
        visitor.environment = environment;
        BoolExpr expr = f.accept(visitor);
        visitor.environment = null;
        return expr;
    }

    protected boolean checkImplies(BoolExpr condition, BoolExpr conclusion) {
        return testUnsatisfiability(negate(mkImp(condition, conclusion)));
    }

    protected boolean checkEquality(BoolExpr b1, BoolExpr b2) {
        return testUnsatisfiability(negate(ctx.mkEq(b1, b2)));
    }

    protected BoolExpr negate(BoolExpr child) {
        return (BoolExpr) ctx.mkNot(child).simplify();
    }

    protected BoolExpr mkImp(BoolExpr condition, BoolExpr consequence) {
        return ctx.mkImplies(condition, consequence);
    }

    protected BoolExpr mkConst(String s) {
        return ctx.mkBoolConst(s);
    }

    private Z3EquivalenceClass probe(Z3EquivalenceClass newClass) {
        for (Z3EquivalenceClass oldClass : alreadyUsed) {
            if (checkEquality(oldClass.expression, newClass.expression)) {
                return oldClass;
            }
        }
        alreadyUsed.add(newClass);
        return newClass;
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

    public class Z3EquivalenceClass implements EquivalenceClass {

        final BoolExpr expression;
        private Formula representative;

        Z3EquivalenceClass(Formula representative, BoolExpr expression) {
            this.representative = representative;
            this.expression = expression;
        }

        @Override
        public Formula getRepresentative() {
            if (representative == null) {
                representative = createRepresentative((BoolExpr) expression.simplify());
            }

            return representative;
        }

        @Override
        public boolean implies(EquivalenceClass equivalenceClass) {
            if (!(equivalenceClass instanceof Z3EquivalenceClass)) {
                return false;
            }

            Z3EquivalenceClass that = (Z3EquivalenceClass) equivalenceClass;
            return checkImplies(expression, that.expression);
        }

        @Override
        public boolean equivalent(EquivalenceClass equivalenceClass) {
            return equals(equivalenceClass);
        }

        @Override
        public EquivalenceClass unfold(boolean unfoldG) {
            return createEquivalenceClass(getRepresentative().unfold(unfoldG));
        }

        @Override
        public EquivalenceClass temporalStep(BitSet valuation) {
            return createEquivalenceClass(getRepresentative().temporalStep(valuation));
        }

        @Override
        public EquivalenceClass and(EquivalenceClass eq) {
            return createEquivalenceClass(new Conjunction(getRepresentative(), eq.getRepresentative()));
        }

        @Override
        public EquivalenceClass or(EquivalenceClass eq) {
            return createEquivalenceClass(new Disjunction(getRepresentative(), eq.getRepresentative()));
        }

        @Override
        public boolean isTrue() {
            return testUnsatisfiability(negate(expression));
        }

        @Override
        public boolean isFalse() {
            return testUnsatisfiability(expression);
        }

        @Override
        public Set<Formula> getSupport() {
            return PropositionVisitor.extractPropositions(representative).keySet();
        }
    }

    private class Z3Visitor implements Visitor<BoolExpr> {

        Function<Formula, Optional<Boolean>> environment;

        @Override
        public BoolExpr visit(BooleanConstant b) {
            return b.value ? TRUE : FALSE;
        }

        @Override
        public BoolExpr visit(Conjunction c) {
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
        public BoolExpr visit(Disjunction d) {
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
        public BoolExpr defaultAction(Formula f) {
            if (environment != null) {
                Optional<Boolean> valuation = environment.apply(f);

                if (valuation.isPresent()) {
                    return valuation.get() ? TRUE : FALSE;
                }
            }

            return mapping.get(f);
        }
    }
}
