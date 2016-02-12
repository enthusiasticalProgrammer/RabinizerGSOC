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

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * @author jkretinsky
 */
public abstract class ModalOperator extends Formula {

    public final Formula operand;

    protected ModalOperator(Formula operand) {
        this.operand = operand;
    }

    @Override
    public String toString() {
        return getOperator() + operand.toString();
    }

    @Override
    // to be overrridden by GOperator
    public Set<GOperator> gSubformulas() {
        return operand.gSubformulas();
    }

    @Override
    // to be overrridden by GOperator
    public Set<GOperator> topmostGs() {
        return operand.topmostGs();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (hashCode() != o.hashCode())
            return false;
        ModalOperator that = (ModalOperator) o;
        return Objects.equals(operand, that.operand);
    }

    @Override
    public Formula temporalStep(Set<String> valuation) {
        return this;
    }

    @Override
    public Set<Formula> getPropositions() {
        Set<Formula> propositions = operand.getPropositions();
        propositions.add(this);
        return propositions;
    }

    @Override
    public Set<String> getAtoms() {
        return operand.getAtoms();
    }

    @Override
    public Formula evaluate(Literal literal) {
        return this;
    }

    @Override
    public Formula evaluate(Set<GOperator> Gs) {
        return this;
    }

    @Override
    public Optional<Literal> getAnUnguardedLiteral() {
        return Optional.empty();
    }

    @Override
    protected int hashCodeOnce() {
        return Objects.hash(operand);
    }

    protected abstract char getOperator();

}
