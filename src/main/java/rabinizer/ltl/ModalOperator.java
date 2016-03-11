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

public abstract class ModalOperator extends ImmutableObject implements Formula {

    public final Formula operand;

    protected ModalOperator(Formula operand) {
        this.operand = operand;
    }

    @Override
    public String toString() {
        return getOperator() + operand.toString();
    }

    @Override // to be overridden by GOperator
    public @NotNull Set<GOperator> gSubformulas() {
        return operand.gSubformulas();
    }

    @Override // to be overridden by GOperator
    public @NotNull Set<GOperator> topmostGs() {
        return operand.topmostGs();
    }

    @Override
    public boolean equals2(ImmutableObject o) {
        ModalOperator that = (ModalOperator) o;
        return Objects.equals(operand, that.operand);
    }

    @Override
    public @NotNull Formula temporalStep(@NotNull Set<String> valuation) {
        return this;
    }

    @Override
    public @NotNull Set<Formula> getPropositions() {
        Set<Formula> propositions = operand.getPropositions();
        propositions.add(this);
        return propositions;
    }

    @Override
    public @NotNull Set<String> getAtoms() {
        return operand.getAtoms();
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

        return build(operand.evaluate(Gs, s));
    }

    @Override
    public Literal getAnUnguardedLiteral() {
        return null;
    }

    protected abstract char getOperator();

    protected abstract ModalOperator build(Formula operand);
}
