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

package rabinizer.automata.buchi;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import rabinizer.automata.output.HOAConsumerBuchi;
import rabinizer.collections.Collections3;
import rabinizer.collections.valuationset.ValuationSetFactory;

import java.util.*;

public class BuchiAutomaton {

    final State initialState;
    final Set<State> states;
    final Set<State> acceptingStates;
    final Table<State, BitSet, Set<State>> transitions;
    final ValuationSetFactory valuationSetFactory;

    BuchiAutomaton(ValuationSetFactory valuationSetFactory) {
        this.valuationSetFactory = valuationSetFactory;
        transitions = HashBasedTable.create();
        states = new HashSet<>();
        acceptingStates = new HashSet<>();
        initialState = new State(null);
        states.add(initialState);
    }

    BuchiAutomaton(BuchiAutomaton automaton) {
        this.valuationSetFactory = automaton.valuationSetFactory;
        transitions = copy(automaton.transitions);
        states = new HashSet<>(automaton.states);
        acceptingStates = new HashSet<>(automaton.acceptingStates);
        initialState = automaton.initialState;
    }

    private static <R, C, V> Table<R, C, Set<V>> copy(Table<R, C, Set<V>> t) {
        Table<R, C, Set<V>> t2 = HashBasedTable.create();

        for (Table.Cell<R, C, Set<V>> cell : t.cellSet()) {
            t2.put(cell.getRowKey(), cell.getColumnKey(), new HashSet<>(cell.getValue()));
        }

        return t2;
    }

    public State createState(String label) {
        State state = new State(label);
        states.add(state);
        return state;
    }

    public void setAccepting(State state) {
        acceptingStates.add(state);
    }

    public State getInitialState() {
        return initialState;
    }

    public void addTransition(State source, BitSet valuation, State target) {
        Set<BuchiAutomaton.State> targets = transitions.get(source, valuation);

        if (targets == null) {
            targets = new HashSet<>();
        }

        targets.add(target);
        transitions.put(source, valuation, targets);
    }

    public Set<State> getTransitions(State source, BitSet valuation) {
        Set<State> successors = transitions.get(source, valuation);

        if (successors == null) {
            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(successors);
    }

    @Override
    public String toString() {
        return "Buchi Automaton:\n" +
                " Q = " + states + '\n' +
                " F = " + acceptingStates + '\n' +
                " d = " + transitions + '\n' +
                " i = " + initialState + '\n';
    }

    public void toHOA(HOAConsumer c) throws HOAConsumerException {
        HOAConsumerBuchi consumer = new HOAConsumerBuchi(c, valuationSetFactory, acceptingStates);

        consumer.setHOAHeader(null);
        consumer.setAcceptanceCondition();
        consumer.setInitialState(initialState);


        for (State s : states) {
            consumer.addState(s);

            for (Map.Entry<BitSet, Set<State>> t : transitions.row(s).entrySet()) {
                for (State s2 : t.getValue()) {
                    consumer.addEdge(t.getKey(), s2);
                }
            }

            consumer.stateDone();
        }

        consumer.done();
    }

    public boolean isDeterministic() {
        Set<State> visited = new HashSet<>();
        Queue<State> workList = new ArrayDeque<>();
        workList.add(initialState);

        while (!workList.isEmpty()) {
            State current = workList.remove();
            visited.add(current);

            for (BitSet valuation : Collections3.powerSet(valuationSetFactory.getSize())) {
                Collection<State> nexts = getTransitions(current, valuation);

                if (nexts.size() > 1) {
                    return false;
                }

                for (State next : nexts) {
                    if (!visited.contains(next)) {
                        workList.add(next);
                    }
                }
            }
        }

        return true;
    }

    public boolean isLimitDeterministic() {
        Set<State> visited = new HashSet<>();
        Queue<State> workList = new ArrayDeque<>(acceptingStates);

        while (!workList.isEmpty()) {
            State current = workList.remove();
            visited.add(current);

            for (BitSet valuation : Collections3.powerSet(valuationSetFactory.getSize())) {
                Collection<State> nexts = getTransitions(current, valuation);

                if (nexts.size() > 1) {
                    return false;
                }

                for (State next : nexts) {
                    if (!visited.contains(next)) {
                        workList.add(next);
                    }
                }
            }
        }

        return true;
    }

    public static class State {
        String label;

        public State(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
