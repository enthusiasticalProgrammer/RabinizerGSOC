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

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Set;

public final class UOperator extends ImmutableObject implements Formula {

    public final Formula left;
    public final Formula right;

    public UOperator(Formula left, Formula right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString() {
        return '(' + left.toString() + 'U' + right.toString() + ')';
    }

    @Override
    public @NotNull Set<GOperator> gSubformulas() {
        Set<GOperator> r = left.gSubformulas();
        r.addAll(right.gSubformulas());
        return r;
    }

    @Override
    public @NotNull Set<GOperator> topmostGs() {
        Set<GOperator> result = left.topmostGs();
        result.addAll(right.topmostGs());
        return result;
    }

    @Override
    public boolean equals2(ImmutableObject o) {
        UOperator uOperator = (UOperator) o;
        return Objects.equals(left, uOperator.left) && Objects.equals(right, uOperator.right);
    }

    @Override
    public @NotNull Formula unfold(boolean unfoldG) {
        // unfold(a U b) = unfold(b) v (unfold(a) ^ X (a U b))
        return new Disjunction(right.unfold(unfoldG), new Conjunction(left.unfold(unfoldG), this));
    }

    @Override
    public @NotNull Formula temporalStep(@NotNull Set<String> valuation) {
        return this;
    }

    @Override
    public @NotNull Formula not() {
        return new Disjunction(new GOperator(right.not()),
                new UOperator(right.not(), new Conjunction(left.not(), right.not())));
    }

    @Override
    public @NotNull Formula evaluate(Literal literal) {
        return this;
    }

    @Override
    public @NotNull Formula evaluate(@NotNull Set<GOperator> Gs, @NotNull EvaluationStrategy s) {
        if (s == EvaluationStrategy.PROPOSITIONAL) {
            return this;
        }

        return new UOperator(left.evaluate(Gs, s), right.evaluate(Gs, s));
    }

    @Override
    public @NotNull Set<Formula> getPropositions() {
        Set<Formula> propositions = left.getPropositions();
        propositions.addAll(right.getPropositions());
        propositions.add(this);
        return propositions;
    }

    @Override
    public @NotNull Set<String> getAtoms() {
        Set<String> atoms = left.getAtoms();
        atoms.addAll(right.getAtoms());
        return atoms;
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
    public boolean isPureEventual() {
        return right.isPureEventual();
    }

    @Override
    public boolean isPureUniversal() {
        return left.isPureUniversal() && right.isPureUniversal();
    }

    @Override
    public boolean isSuspendable() {
        return right.isSuspendable();
    }

    @Override
    protected int hashCodeOnce() {
        return Objects.hash(UOperator.class, left, right);
    }
}
