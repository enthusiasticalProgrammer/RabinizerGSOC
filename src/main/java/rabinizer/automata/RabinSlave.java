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

import rabinizer.collections.valuationset.ValuationSet;
import rabinizer.collections.valuationset.ValuationSetFactory;
import rabinizer.exec.Main;

import java.util.*;

public class RabinSlave extends Automaton<RabinSlave.State> {

    protected final MojmirSlave mojmir;

    public RabinSlave(MojmirSlave mojmir, ValuationSetFactory factory) {
        super(factory);
        this.mojmir = mojmir;
    }

    public void optimizeInitialState() {
        Main.verboseln("Optimizing initial states");
        while (hasSuccessors(initialState) && transitions.values().stream().allMatch(map -> !map.containsKey(initialState))) {
            State oldInit = initialState;
            initialState = getSuccessor(oldInit, new BitSet());
            transitions.remove(oldInit);
        }
    }

    @Override
    protected State generateInitialState() {
        State init = new State();
        init.put(mojmir.getInitialState(), 1);
        return init;
    }

    public class State extends HashMap<MojmirSlave.State, Integer> implements IState<State> {
        private static final long serialVersionUID = 1L;

        @Override
        public String toString() {
            String result = "";
            for (MojmirSlave.State f : keySet()) {
                result += " " + f + "=" + get(f);
            }
            return result;
        }

        @Override
        public BitSet getSensitiveAlphabet() {
            BitSet alphabet = new BitSet();
            this.forEach((state, rank) -> alphabet.or(state.getSensitiveAlphabet()));
            return alphabet;
        }

        @Override
        public ValuationSetFactory getFactory() {
            return valuationSetFactory;
        }

        @Override
        public State getSuccessor(BitSet valuation) {
            State succ = new State();

            // move tokens, keeping the lowest only
            for (MojmirSlave.State currMojmir : keySet()) {
                MojmirSlave.State succMojmir = currMojmir.getSuccessor(valuation);
                if (!mojmir.isSink(succMojmir)) {
                    if (((succ.get(succMojmir) == null) || (succ.get(succMojmir) > get(currMojmir)))) {
                        succ.put(succMojmir, get(currMojmir));
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

            return succ;
        }

        protected ValuationSet getBuyTrans(int rank, Set<MojmirSlave.State> finalStates) {
            ValuationSet buy = valuationSetFactory.createEmptyValuationSet();
            for (Map.Entry<MojmirSlave.State, Integer> stateIntegerEntry : entrySet()) {
                if (stateIntegerEntry.getValue() < rank) {
                    for (MojmirSlave.State fs : keySet()) {
                        for (MojmirSlave.State succ : mojmir.getStates()) {
                            ValuationSet vs1 = mojmir.transitions.get(stateIntegerEntry.getKey()).get(succ);
                            ValuationSet vs2 = mojmir.transitions.get(fs).get(succ);
                            if (!finalStates.contains(succ) && vs1 != null && vs2 != null) {
                                if (!stateIntegerEntry.getKey().equals(fs)) {
                                    vs1 = vs1.clone();
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
    }
}
