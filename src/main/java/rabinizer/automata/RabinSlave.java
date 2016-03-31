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


import rabinizer.collections.valuationset.ValuationSetFactory;
import rabinizer.exec.Main;

import java.util.*;

public class RabinSlave extends Automaton<RabinSlave.State> {

    public final MojmirSlave mojmir;

    public RabinSlave(MojmirSlave mojmir, ValuationSetFactory factory) {
        super(factory, false);
        this.mojmir = mojmir;
    }

    public void optimizeInitialState() {
        // TODO: SCC-Analysis
        while (!transitions.values().contains(initialState) && !transitions.row(initialState).isEmpty()) {
            Main.verboseln("Optimizing initial states");
            State oldInit = initialState;
            initialState = getSuccessor(oldInit, Collections.emptySet());

            edgeBetween.row(oldInit).clear();
            transitions.row(oldInit).clear();
            states.remove(oldInit);
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
        public Set<String> getSensitiveAlphabet() {
            Set<String> alphabet = new HashSet<>();
            this.forEach((state, rank) -> alphabet.addAll(state.getSensitiveAlphabet()));
            return alphabet;
        }

        @Override
        public ValuationSetFactory getFactory() {
            return valuationSetFactory;
        }

        @Override
        public State getSuccessor(Set<String> valuation) {
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
    }
}
