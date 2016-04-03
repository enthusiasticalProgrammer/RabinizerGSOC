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

import com.google.common.collect.Sets;

import java.util.Objects;
import java.util.Set;

public final class Literal extends ImmutableObject implements Formula {

    public final String atom;
    public final boolean negated;

    public Literal(String atom, boolean negated) {
        this.atom = atom;
        this.negated = negated;
    }

    @Override
    public String toString() {
        return negated ? '!' + atom : atom;
    }

    @Override
    public Literal not() {
        return new Literal(atom, !negated);
    }

    @Override
    public Formula temporalStep(Set<String> valuation) {
        return BooleanConstant.get(valuation.contains(atom) ^ negated);
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
    public boolean isPureEventual() {
        return false;
    }

    @Override
    public boolean isPureUniversal() {
        return false;
    }

    @Override
    public boolean isSuspendable() {
        return false;
    }

    @Override
    public boolean equals2(ImmutableObject o) {
        Literal literal = (Literal) o;
        return negated == literal.negated && Objects.equals(atom, literal.atom);
    }

    @Override
    public Formula unfold(boolean unfoldG) {
        return this;
    }

    @Override
    public Literal evaluate(Set<GOperator> Gs, EvaluationStrategy s) {
        return this;
    }

    @Override
    public Set<GOperator> gSubformulas() {
        return Sets.newHashSet();
    }

    @Override
    public Set<GOperator> topmostGs() {
        return Sets.newHashSet();
    }

    @Override
    protected int hashCodeOnce() {
        return Objects.hash(atom, negated);
    }
}
