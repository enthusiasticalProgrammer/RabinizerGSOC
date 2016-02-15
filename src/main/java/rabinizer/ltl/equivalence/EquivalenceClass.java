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

import org.jetbrains.annotations.NotNull;
import rabinizer.ltl.Formula;

import java.util.Set;

/**
 * EquivalenceClass interface.
 * <p>
 * The general contract of this interface is: If two implementing objects were
 * created from different factories, implies and equivalent have to return
 * {@code false}.
 */
public interface EquivalenceClass {

    @NotNull Formula getRepresentative();

    boolean implies(@NotNull EquivalenceClass equivalenceClass);

    /**
     * Check if two classes are equivalent. Implementing classes are expected to
     * implement equivalent and equals, such that they agree on their return
     * values.
     *
     * @param equivalenceClass
     * @return
     */
    boolean equivalent(EquivalenceClass equivalenceClass);

    @NotNull EquivalenceClass unfold(boolean unfoldG);

    @NotNull EquivalenceClass temporalStep(Set<String> valuation);

    @NotNull EquivalenceClass and(@NotNull EquivalenceClass eq);

    @NotNull EquivalenceClass or(@NotNull EquivalenceClass eq);

    boolean isTrue();

    boolean isFalse();

    @NotNull Set<Formula> getSupport();
}