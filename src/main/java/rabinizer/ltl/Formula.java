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

import java.util.Optional;
import java.util.Set;


/**
 * @author Jan Kretinsky
 */
public abstract class Formula {

    private int cachedHashCode;

    @Override
    public int hashCode() {
        if (cachedHashCode == 0) {
            cachedHashCode = hashCodeOnce();
        }

        return cachedHashCode;
    }

    @Override
    public abstract boolean equals(Object o);

    public abstract Set<GOperator> gSubformulas();

    public abstract Set<GOperator> topmostGs();

    /**
     * Unfold temporal operators. This is also called LTL Formula expansion.
     *
     * @param unfoldG If unfoldG is set to true the G-operator is also unfolded.
     *                This is used in for the master transition system.
     * @return The unfolded formula
     */
    public abstract Formula unfold(boolean unfoldG);

    /**
     * Do a single temporal step. This means that one layer of X-operators is
     * removed and literals are replaced by their valuations.
     *
     * @param valuation
     * @return
     */
    public abstract Formula temporalStep(Set<String> valuation);

    public abstract Formula not();

    public abstract Formula evaluate(Literal literal);

    public abstract Formula evaluate(Set<GOperator> Gs);

    public abstract Optional<Literal> getAnUnguardedLiteral();

    /**
     * For the propositional view on LTL modal operators (F, G, U, X) and
     * literals (a, !a) are treated as propositions. The method reduces the set
     * by leaving out the negation of a formula. The propositional reasoning
     * libraries are expected to register negations accordingly.
     *
     * @return
     */
    public abstract Set<Formula> getPropositions();

    public abstract Set<String> getAtoms();

    public abstract <R> R accept(Visitor<R> v);

    public abstract <A, B> A accept(BinaryVisitor<A, B> v, B f);

    public abstract <A, B, C> A accept(TripleVisitor<A, B, C> v, B f, C c);

    // Temporal Properties of an LTL Formula
    public abstract boolean isPureEventual();

    public abstract boolean isPureUniversal();

    public abstract boolean isSuspendable();

    protected abstract int hashCodeOnce();
}
