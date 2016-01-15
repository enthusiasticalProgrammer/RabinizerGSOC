package rabinizer.automata.buchi;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import rabinizer.automata.output.HOAConsumerExtended;
import rabinizer.ltl.ValuationSetFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BuchiAutomaton {

    final State initialState;
    final Set<State> states;
    final Set<State> acceptingStates;
    final Table<State, Set<String>, Set<State>> transitions;
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
            t2.put(cell.getRowKey(), cell.getColumnKey(), new HashSet<V>(cell.getValue()));
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

    public void addTransition(State source, Set<String> valuation, State target) {
        Set<BuchiAutomaton.State> targets = transitions.get(source, valuation);

        if (targets == null) {
            targets = new HashSet<>();
        }

        targets.add(target);
        transitions.put(source, valuation, targets);
    }

    public Set<State> getTransitions(State source, Set<String> valuation) {
        Set<State> successors = transitions.get(source, valuation);

        if (successors == null) {
            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(successors);
    }

    @Override
    public String toString() {
        return "Buchi Automaton:\n" +
                " Q = " + states + "\n" +
                " F = " + acceptingStates + "\n" +
                " d = " + transitions + "\n" +
                " i = " + initialState + "\n";
    }

    public void toHOA(HOAConsumer c) throws HOAConsumerException {
        HOAConsumerExtended<State> consumer = new HOAConsumerExtended<>(c, HOAConsumerExtended.AutomatonType.STATE);

        consumer.setHeader(null, valuationSetFactory.getAlphabet());
        consumer.setInitialState(initialState);
        consumer.setBuchiAcceptance();

        for (State s : states) {
            consumer.addState(s, acceptingStates.contains(s) ? Collections.singletonList(0) : Collections.emptyList());

            for (Map.Entry<Set<String>, Set<State>> t : transitions.row(s).entrySet()) {
                for (State s2 : t.getValue()) {
                    consumer.addEdge(s, t.getKey(), s2);
                }
            }
        }

        consumer.done();
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
