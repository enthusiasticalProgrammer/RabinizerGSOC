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

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @author jkretinsky & Christopher Ziegler
 */
public abstract class PropositionalFormula extends Formula {

    public final Set<Formula> children;

    protected PropositionalFormula(Collection<? extends Formula> children) {
        this.children = ImmutableSet.copyOf(children);
    }

    protected PropositionalFormula(Formula... children) {
        this.children = ImmutableSet.copyOf(children);
    }

    protected PropositionalFormula(Stream<? extends Formula> formulaStream) {
        this.children = ImmutableSet.copyOf(formulaStream.iterator());
    }

    @Override
    public Formula unfold(boolean unfoldG) {
        return create(children.stream().map(c -> c.unfold(unfoldG)));
    }

    @Override
    public Formula evaluate(Literal literal) {
        return create(children.stream().map(c -> c.evaluate(literal)));
    }

    @Override
    public Formula evaluate(Set<GOperator> Gs) {
        return create(children.stream().map(c -> c.evaluate(Gs)));
    }

    @Override
    public Optional<Literal> getAnUnguardedLiteral() {
        for (Formula child : children) {
            Optional<Literal> literal = child.getAnUnguardedLiteral();

            if (literal.isPresent()) {
                return literal;
            }
        }

        return Optional.empty();
    }

    @Override
    public Set<GOperator> topmostGs() {
        return collect(Formula::topmostGs);
    }

    @Override
    public Set<Formula> getPropositions() {
        return collect(Formula::getPropositions);
    }

    @Override
    public Set<GOperator> gSubformulas() {
        return collect(Formula::gSubformulas);
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(3 * children.size());

        s.append('(');

        Iterator<Formula> iter = children.iterator();

        while (iter.hasNext()) {
            s.append(iter.next());

            if (iter.hasNext()) {
                s.append(getOperator());
            }
        }

        s.append(')');

        return s.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (hashCode() != o.hashCode())
            return false;
        PropositionalFormula that = (PropositionalFormula) o;
        return Objects.equals(children, that.children);
    }

    @Override
    public Set<String> getAtoms() {
        return collect(Formula::getAtoms);
    }

    @Override
    public boolean isPureEventual() {
        return allMatch(Formula::isPureEventual);
    }

    @Override
    public boolean isPureUniversal() {
        return allMatch(Formula::isPureUniversal);
    }

    @Override
    public boolean isSuspendable() {
        return allMatch(Formula::isSuspendable);
    }

    @Override
    public Formula temporalStep(Set<String> valuation) {
        return create(children.stream().map(c -> c.temporalStep(valuation)));
    }

    protected abstract PropositionalFormula create(Stream<? extends Formula> formulaStream);

    @Override
    protected int hashCodeOnce() {
        return Objects.hash(children);
    }

    protected abstract char getOperator();

    private <E> Set<E> collect(Function<Formula, Collection<E>> f) {
        Set<E> set = new HashSet<>(children.size());
        children.forEach(c -> set.addAll(f.apply(c)));
        return set;
    }

    public boolean allMatch(Predicate<Formula> p) {
        return children.stream().allMatch(p);
    }

    public boolean anyMatch(Predicate<Formula> p) {
        return children.stream().anyMatch(p);
    }
}
