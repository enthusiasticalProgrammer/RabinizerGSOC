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

import java.util.Optional;
import java.util.Set;

public interface Formula {
    @NotNull Set<GOperator> gSubformulas();

    @NotNull Set<GOperator> topmostGs();

    /**
     * Unfold temporal operators. This is also called LTL ImmutableObject expansion.
     *
     * @param unfoldG If unfoldG is set to true the G-operator is also unfolded.
     *                This is used in for the master transition system.
     * @return The unfolded formula
     */
    @NotNull Formula unfold(boolean unfoldG);

    /**
     * Do a single temporal step. This means that one layer of X-operators is
     * removed and literals are replaced by their valuations.
     *
     * @param valuation
     * @return
     */
    @NotNull Formula temporalStep(@NotNull Set<String> valuation);

    @NotNull Formula not();

    @NotNull Formula evaluate(Literal literal);

    default @NotNull Formula evaluate(@NotNull Set<GOperator> Gs) {
        return evaluate(Gs, EvaluationStrategy.PROPOSITIONAL);
    }

    @NotNull Formula evaluate(@NotNull Set<GOperator> Gs, @NotNull EvaluationStrategy s);

    Literal getAnUnguardedLiteral();

    /**
     * For the propositional view on LTL modal operators (F, G, U, X) and
     * literals (a, !a) are treated as propositions.
     *
     * @return
     */
    @NotNull Set<Formula> getPropositions();

    @NotNull Set<String> getAtoms();

    <R> R accept(Visitor<R> v);

    <A, B> A accept(BinaryVisitor<A, B> v, B f);

    <A, B, C> A accept(TripleVisitor<A, B, C> v, B f, C c);

    // Temporal Properties of an LTL ImmutableObject
    boolean isPureEventual();

    boolean isPureUniversal();

    boolean isSuspendable();

    enum EvaluationStrategy {
        PROPOSITIONAL, LTL
    }
}
