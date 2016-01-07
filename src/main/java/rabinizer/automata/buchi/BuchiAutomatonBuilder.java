package rabinizer.automata.buchi;

import jhoafparser.ast.AtomAcceptance;
import jhoafparser.ast.AtomLabel;
import jhoafparser.ast.BooleanExpression;
import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import rabinizer.automata.buchi.BuchiAutomaton.State;
import rabinizer.ltl.Literal;
import rabinizer.ltl.ValuationSet;
import rabinizer.ltl.ValuationSetFactory;
import rabinizer.ltl.bdd.BDDValuationSetFactory;

import java.util.*;

public class BuchiAutomatonBuilder implements HOAConsumer {

    private final Deque<BuchiAutomaton> automata = new ArrayDeque<>();
    private BuchiAutomaton automaton;
    private ValuationSetFactory<String> valuationSetFactory;
    private Integer initialState;
    private String[] integerToLetter;
    private State[] integerToState;
    private int implicitEdgeCounter;

    @Override
    public boolean parserResolvesAliases() {
        return false;
    }

    @Override
    public void notifyHeaderStart(String s) {
        valuationSetFactory = null;
        integerToLetter = null;
        integerToState = null;
        initialState = null;
        automaton = null;
    }

    @Override
    public void setNumberOfStates(int i) throws HOAConsumerException {
        integerToState = new State[i];
    }

    @Override
    public void addStartStates(List<Integer> list) throws HOAConsumerException {
        if (list.size() != 1 || initialState != null) {
            throw new HOAConsumerException("Only a single initial state is supported.");
        }

        initialState = list.get(0);
    }

    @Override
    public void addAlias(String s, BooleanExpression<AtomLabel> booleanExpression) throws HOAConsumerException {
        throw new HOAConsumerException("Unsupported Operation.");
    }

    @Override
    public void setAPs(List<String> list) throws HOAConsumerException {
        integerToLetter = new String[list.size()];
        list.toArray(integerToLetter);
        valuationSetFactory = new BDDValuationSetFactory(list);
    }

    @Override
    public void setAcceptanceCondition(int i, BooleanExpression<AtomAcceptance> booleanExpression) throws HOAConsumerException {
        if (i != 1) {
            throw new HOAConsumerException("Unsupported Acceptance Conditions: " + i + ' ' + booleanExpression);
        }
    }

    @Override
    public void provideAcceptanceName(String s, List<Object> list) throws HOAConsumerException {
        if (!"Buchi".equals(s)) {
            throw new HOAConsumerException("Unsupported Acceptance Name: " + s);
        }
    }

    @Override
    public void setName(String s) throws HOAConsumerException {
        // No operation
    }

    @Override
    public void setTool(String s, String s1) throws HOAConsumerException {
        // No operation
    }

    @Override
    public void addProperties(List<String> list) throws HOAConsumerException {
        // No operation
    }

    @Override
    public void addMiscHeader(String s, List<Object> list) throws HOAConsumerException {
        // No operation
    }

    @Override
    public void notifyBodyStart() throws HOAConsumerException {
        automaton = new BuchiAutomaton(valuationSetFactory);
        ensureSpaceInMap(initialState);
        automaton.getInitialState().label = Integer.toString(initialState);
        integerToState[initialState] = automaton.getInitialState();
    }

    @Override
    public void addState(int i, String s, BooleanExpression<AtomLabel> booleanExpression, List<Integer> list) throws HOAConsumerException {
        State state = addState(s, i);
        if (list != null) {
            if (list.size() > 1) {
                throw new HOAConsumerException("Unsupported acceptance");
            }

            if (list.contains(0)) {
                automaton.setAccepting(state);
            }
        }
    }

    @Override
    public void addEdgeImplicit(int i, List<Integer> list, List<Integer> list1) throws HOAConsumerException {
        addEdgeWithLabel(i, BooleanExpression.fromImplicit(implicitEdgeCounter, valuationSetFactory.getAlphabet().size()), list, list1);
        implicitEdgeCounter++;
    }

    @Override
    public void addEdgeWithLabel(int i, BooleanExpression<AtomLabel> booleanExpression, List<Integer> list, List<Integer> list1) throws HOAConsumerException {
        State source = integerToState[i];

        if (source == null) {
            source = addState(null, i);
        }

        if (list1 != null && !list1.isEmpty()) {
            throw new HOAConsumerException("Edge acceptance not supported");
        }

        if (list.size() > 1) {
            throw new HOAConsumerException("Unsupported transitions targets");
        }

        if (!list.isEmpty()) {
            State target = integerToState[list.get(0)];

            if (target == null) {
                target = addState(null, list.get(0));
            }

            ValuationSet valuationSet = toValuationSet(booleanExpression);

            for (Set<String> valuation : valuationSet) {
                automaton.addTransition(source, valuation, target);
            }
        }
    }

    @Override
    public void notifyEndOfState(int i) throws HOAConsumerException {
        implicitEdgeCounter = 0;
    }

    @Override
    public void notifyEnd() throws HOAConsumerException {
        automata.add(automaton);
        notifyHeaderStart(null);
    }

    @Override
    public void notifyAbort() {
        notifyHeaderStart(null);
    }

    @Override
    public void notifyWarning(String s) throws HOAConsumerException {
        // No operation
    }

    public List<BuchiAutomaton> getAutomata() {
        return new ArrayList(automata);
    }

    private void ensureSpaceInMap(int id) {
        if (integerToState == null) {
            integerToState = new State[id + 1];
        }

        if (id >= integerToState.length) {
            integerToState = Arrays.copyOf(integerToState, id + 1);
        }
    }

    private State addState(String label, int id) {
        ensureSpaceInMap(id);

        if (integerToState[id] == null) {
            integerToState[id] = automaton.createState(label == null ? Integer.toString(id) : label);
        }

        if (integerToState[id].label == null) {
            integerToState[id].label = label;
        }

        return integerToState[id];
    }

    private ValuationSet toValuationSet(BooleanExpression<AtomLabel> label) {
        if (label.isFALSE()) {
            return valuationSetFactory.createEmptyValuationSet();
        }

        if (label.isTRUE()) {
            return valuationSetFactory.createUniverseValuationSet();
        }

        if (label.isAtom()) {
            Literal literal = new Literal(integerToLetter[label.getAtom().getAPIndex()], false);
            ValuationSet valuationSet = valuationSetFactory.createUniverseValuationSet();
            valuationSet.restrictWith(literal);
            return valuationSet;
        }

        if (label.isNOT()) {
            return toValuationSet(label.getLeft()).complement();
        }

        if (label.isAND()) {
            ValuationSet valuationSet = toValuationSet(label.getLeft());
            valuationSet.retainAll(toValuationSet(label.getRight()));
            return valuationSet;
        }

        if (label.isOR()) {
            ValuationSet valuationSet = toValuationSet(label.getLeft());
            valuationSet.addAll(toValuationSet(label.getRight()));
            return valuationSet;
        }

        throw new IllegalArgumentException("Unsupported Case: " + label);
    }
}
