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

import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import omega_automaton.Edge;
import omega_automaton.algorithms.SCCAnalyser;
import omega_automaton.collections.valuationset.ValuationSet;
import omega_automaton.collections.valuationset.ValuationSetFactory;
import omega_automaton.output.HOAConsumerExtended;
import rabinizer.automata.Product.ProductState;

public final class FrequencySelfProductSlave extends AbstractSelfProductSlave<FrequencySelfProductSlave.State> {

    protected FrequencySelfProductSlave(MojmirSlave mojmir, ValuationSetFactory factory) {
        super(mojmir, factory);
    }

    @Override
    protected State generateState(Map<MojmirSlave.State, Integer> map) {
        State s = new State();
        map.forEach((a, b) -> s.put(a, b));
        return s;
    }

    @Override
    public final void toHOABody(HOAConsumerExtended hoa) {
        for (State s : getStates()) {
            hoa.addState(s);
            getSuccessors(s).forEach((k, v) -> hoa.addEdge(v, k.successor));
            toHOABodyEdge(s, hoa);
            hoa.stateDone();
        }
    }

    public class State extends AbstractSelfProductSlave<State>.State {

        @Override
        public Edge<State> getSuccessor(BitSet valuation) {
            State successor = new State();

            // Move tokens, make use of acyclicity:
            List<Set<MojmirSlave.State>> SCCStates = SCCAnalyser.SCCsStates(mojmir);
            SCCStates.stream().forEach(set -> set.removeIf(state -> mojmir.isSink(state)));
            SCCStates.removeIf(set -> set.isEmpty());

            for (Set<MojmirSlave.State> stateSet : SCCStates) {
                if (stateSet.size() != 1) {
                    throw new RuntimeException("The size of this set is not one. This means that the MojmirSlave is not acyclic, which means that there is a bug.");
                }
                stateSet.stream().forEach(s -> {
                    MojmirSlave.State succMojmir = s.getSuccessor(valuation).successor;
                    if(!mojmir.isSink(succMojmir)){
                        successor.put(succMojmir, (this.get(s) == null ? 0 : this.get(s)) + (successor.get(succMojmir) == null ? 0 : successor.get(succMojmir)));
                    }
                    successor.put(s, 0);
                });
            }

            // add initial token
            successor.put(mojmir.getInitialState(), 1 + (successor.get(mojmir.getInitialState()) == null ? 0 : successor.get(mojmir.getInitialState())));
            return new Edge<>(successor, new BitSet(0));
        }
    }
}
