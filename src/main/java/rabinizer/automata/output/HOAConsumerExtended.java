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
import rabinizer.automata.output.HOAConsumerExtended.AccType;
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
public class HOAConsumerExtended<S, C> {

    protected final HOAConsumer hoa;
    protected final C acc;
    protected S currentState;
    protected ValuationSetFactory valuationSetFactory;

    private final Map<S, Integer> stateNumbers;

    public HOAConsumerExtended(HOAConsumer hoa, ValuationSetFactory valSetFac, C accCond) {
        this.hoa = hoa;
        stateNumbers = new HashMap<>();
        valuationSetFactory = valSetFac;
        acc=accCond;
    }

    protected AccType getAccCondition() {
        return AccType.ALL;
    }

    public void setAcceptanceCondition() throws HOAConsumerException {
        hoa.provideAcceptanceName(getAccCondition().toString(), Collections.emptyList());
    }

    protected static BooleanExpression<AtomAcceptance> mkInf(int number) {
        return new BooleanExpression<>(new AtomAcceptance(AtomAcceptance.Type.TEMPORAL_INF, number, false));
    }

    public void setHOAHeader(String info) {
        try {
            hoa.notifyHeaderStart("v1");
            hoa.setTool("Rabinizer", "infty");
            hoa.setName("Automaton for " + info);
            hoa.setAPs(IntStream.range(0, valuationSetFactory.getSize()).mapToObj(i -> valuationSetFactory.getAliases().inverse().get(i)).collect(Collectors.toList()));
        } catch (HOAConsumerException ex) {
            // We wrap HOAConsumerException into an unchecked exception in order to keep the interfaces clean and tidy.
            throw new RuntimeException(ex);
        }
    }

    public static void doHOAStatesEmpty(HOAConsumer hoa) {
        try {
            hoa.notifyHeaderStart("v1");
            hoa.setTool("Rabinizer", "infty");
            hoa.setName("Automaton for false");
            hoa.setAPs(Collections.emptyList());
            hoa.setAcceptanceCondition(0, new BooleanExpression<>(false));
            hoa.notifyBodyStart();
            hoa.notifyEnd();
        } catch (HOAConsumerException ex) {
            // We wrap HOAConsumerException into an unchecked exception in order to keep the interfaces clean and tidy.
            throw new RuntimeException(ex);
        }x
    }

    public void setInitialState(S initialState) {
        try {
            hoa.addStartStates(Collections.singletonList(getStateId(initialState)));
        } catch (HOAConsumerException ex) {
            // We wrap HOAConsumerException into an unchecked exception in order to keep the interfaces clean and tidy.
            throw new RuntimeException(ex);
        }
    }

    public void addState(S state) {
        try {
            if (currentState == null) {
                hoa.notifyBodyStart();
            }

            currentState = state;
            hoa.addState(getStateId(state), state.toString(), null, null);
        } catch (HOAConsumerException ex) {
            // We wrap HOAConsumerException into an unchecked exception in order to keep the interfaces clean and tidy.
            throw new RuntimeException(ex);
        }
    }

    public void stateDone() {
        try {
            hoa.notifyEndOfState(getStateId(currentState));
        } catch (HOAConsumerException ex) {
            // We wrap HOAConsumerException into an unchecked exception in order to keep the interfaces clean and tidy.
            throw new RuntimeException(ex);
        }
    }

    public void done() {
        try {
            hoa.notifyEnd();
        } catch (HOAConsumerException ex) {
            // We wrap HOAConsumerException into an unchecked exception in order to keep the interfaces clean and tidy.
            throw new RuntimeException(ex);
        }
    }

    protected void addEdgeBackend(ValuationSet label, S end, List<Integer> accSets) {
        if (label.isEmpty()) {
            return;
        }

        try {
            hoa.addEdgeWithLabel(getStateId(currentState), label.toFormula().accept(new FormulaConverter()), Collections.singletonList(getStateId(end)), accSets);
        } catch (HOAConsumerException ex) {
            // We wrap HOAConsumerException into an unchecked exception in order to keep the interfaces clean and tidy.
            throw new RuntimeException(ex);
        }
    }

    public void addEdge(ValuationSet key, S end) {
        addEdgeBackend(key, end, null);
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
