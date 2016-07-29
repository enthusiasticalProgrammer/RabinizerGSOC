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

import omega_automaton.Edge;
import omega_automaton.collections.valuationset.ValuationSet;
import omega_automaton.collections.valuationset.ValuationSetFactory;
import rabinizer.exec.Main;

import java.util.*;

final class RabinSlave extends AbstractSelfProductSlave<RabinSlave.State> {

    RabinSlave(MojmirSlave mojmir, ValuationSetFactory factory) {
        super(mojmir, factory);
    }

    void optimizeInitialState() {
        Main.verboseln("Optimizing initial states");
        while (hasSuccessors(initialState)
                && transitions.values().stream().allMatch(map -> !map.keySet().stream().map(edge -> edge.successor).anyMatch(state -> state.equals(initialState)))) {
            State oldInit = initialState;
            initialState = getSuccessor(oldInit, new BitSet()).successor;
            transitions.remove(oldInit);
        }
    }

    @Override
    protected State generateState(Map<MojmirSlave.State, Integer> map) {
        State s = new State();
        map.forEach((a, b) -> s.put(a, b));
        return s;
    }

    public class State extends AbstractSelfProductSlave<State>.State {
        @Override
        public Edge<State> getSuccessor(BitSet valuation) {
            State succ = new State();

            // move tokens, keeping the lowest only
            for (MojmirSlave.State currMojmir : keySet()) {
                Edge<MojmirSlave.State> succMojmir = currMojmir.getSuccessor(valuation);
                if (!mojmir.isSink(succMojmir.successor)) {
                    if (((succ.get(succMojmir.successor) == null) || (succ.get(succMojmir.successor) > get(currMojmir)))) {
                        succ.put(succMojmir.successor, get(currMojmir));
                    }
                }
            }

            int[] tokens = new int[succ.keySet().size()];
            int i = 0;
            for (Entry<MojmirSlave.State, Integer> stateIntegerEntry : succ.entrySet()) {
                tokens[i] = stateIntegerEntry.getValue();
                i++;
            }
            Arrays.sort(tokens);
            for (Entry<MojmirSlave.State, Integer> stateIntegerEntry : succ.entrySet()) {
                for (int j = 0; j < tokens.length; j++) {
                    if (stateIntegerEntry.getValue().equals(tokens[j])) {
                        succ.put(stateIntegerEntry.getKey(), j + 1);
                    }
                }
            }

            if (!succ.containsKey(mojmir.getInitialState())) {
                succ.put(mojmir.getInitialState(), succ.keySet().size() + 1);
            }

            return new Edge<>(succ, new BitSet(0));
        }

        ValuationSet getBuyTrans(int rank, Set<MojmirSlave.State> finalStates) {
            ValuationSet buy = valuationSetFactory.createEmptyValuationSet();
            for (Map.Entry<MojmirSlave.State, Integer> stateIntegerEntry : entrySet()) {
                if (stateIntegerEntry.getValue() < rank) {
                    for (MojmirSlave.State fs : keySet()) {
                        for (MojmirSlave.State succ : mojmir.getStates()) {
                            ValuationSet vs1 = getValuationForBuyTrans(stateIntegerEntry.getKey(), succ);
                            ValuationSet vs2 = getValuationForBuyTrans(fs, succ);
                            if (!finalStates.contains(succ) && vs1 != null && vs2 != null) {
                                if (!stateIntegerEntry.getKey().equals(fs)) {
                                    vs1 = vs1.copy();
                                    vs1.retainAll(vs2);
                                    buy.addAll(vs1);
                                } else if (succ.equals(mojmir.getInitialState())) {
                                    buy.addAll(vs1);
                                }

                            }
                        }
                    }
                }
            }
            return buy;
        }

        private ValuationSet getValuationForBuyTrans(MojmirSlave.State predecessor, MojmirSlave.State successor) {
            Map<Edge<MojmirSlave.State>, ValuationSet> successors = mojmir.getSuccessors(predecessor);
            ValuationSet result = valuationSetFactory.createEmptyValuationSet();

            successors.forEach((k, v) -> {
                if (k.successor.equals(successor)) {
                    result.addAll(v);
                }
            });

            return result;
        }
    }
}
