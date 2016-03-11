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
import java.util.stream.Collectors;

public interface IState<S> {

    /**
     * @param valuation
     * @return null is returned if the transition would move to a non-accepting BSCC.
     */
    @Nullable S getSuccessor(@NotNull Set<String> valuation);

    default @NotNull Map<ValuationSet, S> getSuccessors() {
        Map<ValuationSet, S> successors = new HashMap<>();

        for (ValuationSet valuationSet : partitionSuccessors()) {
            S successor = getSuccessor(valuationSet.pickAny());

            if (successor != null) {
                successors.put(valuationSet, successor);
            }
        }

        return successors;
    }

    default @NotNull Set<ValuationSet> partitionSuccessors() {
        Set<String> sensitiveAlphabet = getSensitiveAlphabet();
        ValuationSetFactory factory = getFactory();
        return Sets.powerSet(sensitiveAlphabet).stream().map(subset -> factory.createValuationSet(subset, sensitiveAlphabet)).collect(Collectors.toSet());
    }

    @NotNull Set<String> getSensitiveAlphabet();

    @NotNull ValuationSetFactory getFactory();
}
