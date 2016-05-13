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
import jhoafparser.ast.BooleanExpression;
import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import rabinizer.collections.valuationset.ValuationSet;
import rabinizer.collections.valuationset.ValuationSetFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *
 * @param <S> type of states
 * @param <C> type of acceptance condition
 */
public abstract class HOAConsumerExtended<S, C> {

    protected final HOAConsumer hoa;
    protected S currentState;
    protected ValuationSetFactory valuationSetFactory;

    private final Map<S, Integer> stateNumbers;

    public HOAConsumerExtended(HOAConsumer hoa, ValuationSetFactory valSetFac) {
        this.hoa = hoa;
        stateNumbers = new HashMap<>();
        valuationSetFactory = valSetFac;
    }

    public abstract void setAcceptanceCondition(C acc) throws HOAConsumerException;

    protected static BooleanExpression<AtomAcceptance> mkInf(int number) {
        return new BooleanExpression<>(new AtomAcceptance(AtomAcceptance.Type.TEMPORAL_INF, number, false));
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

    protected void addEdgeBackend(ValuationSet label, S end, List<Integer> accSets) throws HOAConsumerException {
        if (label.isEmpty()) {
            return;
        }

        hoa.addEdgeWithLabel(getStateId(currentState), label.toFormula().accept(new FormulaConverter()), Collections.singletonList(getStateId(end)), accSets);
    }

    protected int getStateId(S state) {
        if (!stateNumbers.containsKey(state)) {
            stateNumbers.put(state, stateNumbers.size());
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
