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

package rabinizer.automata.output;

import jhoafparser.ast.AtomAcceptance;
import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import rabinizer.automata.TranSet;
import rabinizer.collections.valuationset.ValuationSetFactory;
import rabinizer.ltl.Formula;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * S stands for the class of states C for the class of acceptance condition
 */
public abstract class HOAConsumerExtended<S, C> {

    protected final HOAConsumer hoa;

    private final Map<S, Integer> stateNumbers;
    protected final Map<TranSet<S>, Integer> acceptanceNumbers;

    private S currentState;
    protected ValuationSetFactory valuationSetFactory;

    public HOAConsumerExtended(HOAConsumer hoa, ValuationSetFactory valSetFac) {
        this.hoa = hoa;
        stateNumbers = new HashMap<>();
        acceptanceNumbers = new HashMap<>();
        valuationSetFactory = valSetFac;
    }

    protected abstract AccType getAccCondition(C acc);

    protected abstract void setAccCondForHOAConsumer(C acc) throws HOAConsumerException;

    protected static AtomAcceptance mkInf(int number) {
        return new AtomAcceptance(AtomAcceptance.Type.TEMPORAL_INF, number, false);
    }

    public void setHOAHeader(String info) throws HOAConsumerException {
        hoa.notifyHeaderStart("v1");
        hoa.setTool("Rabinizer", "infty");
        hoa.setName("Automaton for " + info);
        hoa.setAPs(IntStream.range(0, valuationSetFactory.getSize()).mapToObj(i -> valuationSetFactory.getAliases().inverse().get(i)).collect(Collectors.toList()));
    }

    public void setInitialState(S initialState) throws HOAConsumerException {
        hoa.addStartStates(Collections.singletonList(getStateId(initialState)));
    }

    public void setAcceptanceCondition(C acc) throws HOAConsumerException {
        AccType accT = getAccCondition(acc);
        hoa.provideAcceptanceName(accT.toString(), Collections.emptyList());
        setAccCondForHOAConsumer(acc);
    }

    public void addState(S state) throws HOAConsumerException {
        if (currentState == null) {
            hoa.notifyBodyStart();
        }
        currentState = state;
        hoa.addState(getStateId(state), state.toString(), null, null);
    }

    public void stateDone() throws HOAConsumerException {
        hoa.notifyEndOfState(getStateId(currentState));
    }

    public void done() throws HOAConsumerException {
        hoa.notifyEnd();
    }

    protected void addEdgeBackend(S begin, Formula label, S end, List<Integer> accSets) throws HOAConsumerException {
        hoa.addEdgeWithLabel(getStateId(begin), label.accept(new FormulaConverter()), Collections.singletonList(getStateId(end)), accSets);
    }

    protected Integer getTranSetId(TranSet<S> o) {
        if (!acceptanceNumbers.containsKey(o)) {
            acceptanceNumbers.put(o, acceptanceNumbers.size());
        }
        return acceptanceNumbers.get(o);
    }

    protected int getStateId(S state) {
        if (!stateNumbers.containsKey(state)) {
            stateNumbers.put(state, stateNumbers.keySet().size());
        }

        return stateNumbers.get(state);
    }


    public enum AccType {
        NONE, ALL, BUCHI, COBUCHI, GENBUCHI, RABIN, GENRABIN;

        @Override
        public String toString() {
            switch (this) {
                case NONE:
                    return "none";

                case ALL:
                    return "all";

                case BUCHI:
                    return "Buchi";

                case COBUCHI:
                    return "co-Buchi";

                case GENBUCHI:
                    return "generalized-Buchi";

                case RABIN:
                    return "Rabin";

                case GENRABIN:
                    return "generalized-Rabin";

                default:
                    throw new AssertionError();
            }
        }
    }
}
