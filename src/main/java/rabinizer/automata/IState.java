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

package rabinizer.automata;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rabinizer.collections.valuationset.ValuationSet;
import rabinizer.collections.valuationset.ValuationSetFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public interface IState<S> {

    /**
     * @param valuation
     * @return null is returned if the transition would move to a non-accepting BSCC.
     */
    @Nullable S getSuccessor(@NotNull Set<String> valuation);

    default @NotNull Map<ValuationSet, S> getSuccessors() {
        Map<ValuationSet, S> successors = new HashMap<>();

        Set<String> sensitiveAlphabet = getSensitiveAlphabet();
        ValuationSetFactory factory = getFactory();

        for (Set<String> valuation : Sets.powerSet(sensitiveAlphabet)) {
            S successor = getSuccessor(valuation);

            if (successor != null) {
                successors.put(factory.createValuationSet(valuation, sensitiveAlphabet), successor);
            }
        }

        return successors;
    }

    @NotNull Set<String> getSensitiveAlphabet();

    @NotNull ValuationSetFactory getFactory();
}
