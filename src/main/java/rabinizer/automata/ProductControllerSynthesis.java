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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import ltl.FrequencyG;
import ltl.GOperator;
import ltl.UnaryModalOperator;
import omega_automaton.collections.TranSet;
import omega_automaton.collections.valuationset.ValuationSet;
import omega_automaton.collections.valuationset.ValuationSetFactory;
import rabinizer.frequencyLTL.FOperatorForMojmir;

public class ProductControllerSynthesis extends Product<FrequencySelfProductSlave.State> {

    protected final Map<UnaryModalOperator, FrequencySelfProductSlave> secondaryAutomata;

    public ProductControllerSynthesis(Master primaryAutomaton, Map<UnaryModalOperator, FrequencySelfProductSlave> slaves, ValuationSetFactory factory,
            Collection<Optimisation> optimisations) {
        super(primaryAutomaton, factory, optimisations);
        this.secondaryAutomata = slaves;
    }

    @Override
    protected final State generateInitialState() {
        return new State(primaryAutomaton.getInitialState(), relevantSecondarySlaves(primaryAutomaton.getInitialState()), k -> secondaryAutomata.get(k).getInitialState());
    }

    @Override
    protected Map<UnaryModalOperator, FrequencySelfProductSlave> getSecondaryAutomata() {
        return secondaryAutomata;
    }

    @Override
    protected Set<UnaryModalOperator> getKeys() {
        return secondaryAutomata.keySet();
    }

    public TranSet<ProductState> getControllerAcceptanceF(FOperatorForMojmir f, Set<MojmirSlave.State> finalStates) {
        return getSucceedingProductTransitions(secondaryAutomata.get(f), -1, finalStates);
    }

    public TranSet<ProductState> getControllerAcceptanceG(GOperator g, Set<MojmirSlave.State> finalStates) {
        return getFailingProductTransitions(secondaryAutomata.get(g), finalStates);
    }

    public Map<TranSet<ProductState>, Integer> getControllerAcceptanceFrequencyG(FrequencyG g, Set<MojmirSlave.State> finalStates) {
        Map<TranSet<ProductState>, Integer> result = new HashMap<>();
        int maxTokenNumber = secondaryAutomata.get(g).mojmir.size();
        for (int i = 0; i <= maxTokenNumber; i++) {
            TranSet<ProductState> relevantTransitions = getSucceedingProductTransitions(secondaryAutomata.get(g), i, finalStates);
            if (!relevantTransitions.isEmpty()) {
                result.put(relevantTransitions, i);
            }
        }
        return result;
    }

    public class State extends Product<FrequencySelfProductSlave.State>.ProductState {

        protected State(Master.State primaryState, Collection<UnaryModalOperator> keys, Function<UnaryModalOperator, FrequencySelfProductSlave.State> constructor) {
            super(primaryState, keys, constructor);
        }

        protected State(Master.State primaryState, ImmutableMap<UnaryModalOperator, FrequencySelfProductSlave.State> secondaryStates) {
            super(primaryState, secondaryStates);
        }

        @Override
        protected Map<UnaryModalOperator, FrequencySelfProductSlave> getSecondaryAutomata() {
            return secondaryAutomata;
        }

        @Override
        protected State constructState(Master.State primaryState, ImmutableMap<UnaryModalOperator, FrequencySelfProductSlave.State> secondaryStates) {
            return new State(primaryState, secondaryStates);
        }

        @Override
        public ValuationSet getSucceedTransitions(MojmirSlave mojmir, int rank, Set<MojmirSlave.State> finalStates) {
            ValuationSet succeed = valuationSetFactory.createEmptyValuationSet();
            FrequencySelfProductSlave.State rs = secondaryStates.get(mojmir.label);
            if (rs != null) { // relevant slave
                if (rank == -1) {
                    for (Map.Entry<MojmirSlave.State, Integer> stateIntegerEntry : rs.entrySet()) {
                        succeed.addAll(stateIntegerEntry.getKey().getSucceedMojmirTransitions(finalStates));
                    }
                    return succeed;
                } else {
                    Map<MojmirSlave.State, ValuationSet> succeedingTransitions = new HashMap<>();
                    for (MojmirSlave.State state : rs.keySet()) {
                        ValuationSet succeedingForState = state.getSucceedMojmirTransitions(finalStates);
                        if (!succeedingForState.isEmpty()) {
                            succeedingTransitions.put(state, state.getSucceedMojmirTransitions(finalStates));
                        }
                    }

                    for (Set<MojmirSlave.State> stateSet : Sets.powerSet(succeedingTransitions.keySet())) {
                        if (stateSet.stream().mapToInt(s -> rs.get(s)).sum() == rank) {
                            ValuationSet successor = valuationSetFactory.createUniverseValuationSet();
                            for (MojmirSlave.State state : stateSet) {
                                successor.retainAll(succeedingTransitions.get(state));
                            }
                            removeAllTransitionsCoveredByALargerRank(rank, rs, succeedingTransitions, successor);
                            succeed.addAll(successor);
                        }
                    }
                }
            }

            return succeed;
        }

        private void removeAllTransitionsCoveredByALargerRank(int rank, FrequencySelfProductSlave.State rs, Map<MojmirSlave.State, ValuationSet> succeedingTransitions,
                ValuationSet successor) {
            for (Set<MojmirSlave.State> set : Sets.powerSet(succeedingTransitions.keySet())) {
                if (set.stream().mapToInt(s -> rs.get(s)).sum() > rank) {
                    for (MojmirSlave.State state : set) {
                        successor.removeAll(succeedingTransitions.get(state));
                    }
                }
            }
        }

    }
}
