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

package rabinizer.ltl;


import com.google.common.collect.ImmutableSet;
import rabinizer.collections.Collections3;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Stream;

public final class Disjunction extends PropositionalFormula {

    public Disjunction(ImmutableSet<Formula> conjuncts) { super(conjuncts);}

    public Disjunction(Collection<? extends Formula> disjuncts) {
        super(disjuncts);
    }

    public Disjunction(Formula... disjuncts) {
        super(disjuncts);
    }

    public Disjunction(Stream<? extends Formula> formulaStream) {
        super(formulaStream);
    }

    @Override
    public Formula not() {
        return new Conjunction(children.stream().map(Formula::not));
    }

    @Override
    public void accept(VoidVisitor v) {
        v.visit(this);
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }

    @Override
    public <A, B> A accept(BinaryVisitor<A, B> v, B f) {
        return v.visit(this, f);
    }

    @Override
    public <A, B, C> A accept(TripleVisitor<A, B, C> v, B f, C c) {
        return v.visit(this, f, c);
    }

    @Override
    public Formula unfold(boolean unfoldG) {
        return create(children.stream().map(c -> c.unfold(unfoldG)));
    }

    @Override
    public Formula evaluate(Set<GOperator> Gs, EvaluationStrategy s) {
        return create(children.stream().map(c -> c.evaluate(Gs, s)));
    }

    @Override
    public Formula temporalStep(Set<String> valuation) {
        return create(children.stream().map(c -> c.temporalStep(valuation)));
    }

    @Override
    protected char getOperator() {
        return '|';
    }

    public static Formula create(Formula... formulaStream) {
        return create(Arrays.stream(formulaStream));
    }

    public static Formula create(Stream<? extends Formula> formulaStream) {
        Iterator<? extends Formula> iterator = formulaStream.iterator();
        ImmutableSet.Builder<Formula> builder = ImmutableSet.builder();

        while (iterator.hasNext()) {
            Formula child = iterator.next();

            if (child == BooleanConstant.TRUE) {
                return BooleanConstant.TRUE;
            }

            if (child == BooleanConstant.FALSE) {
                continue;
            }

            if (child instanceof Disjunction) {
                builder.addAll(((Disjunction) child).children);
            } else {
                builder.add(child);
            }
        }

        ImmutableSet<Formula> set = builder.build();

        if (set.isEmpty()) {
            return BooleanConstant.FALSE;
        }

        if (Collections3.isSingleton(set)) {
            return Collections3.getElement(set);
        }

        return new Disjunction(set);
    }

    @Override
    protected int hashCodeOnce() {
        return Objects.hash(Disjunction.class, children);
    }
}
