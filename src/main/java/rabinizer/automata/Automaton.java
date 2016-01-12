package rabinizer.automata;

import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

import jhoafparser.consumer.HOAConsumerException;
import jhoafparser.consumer.HOAConsumerPrint;
import rabinizer.automata.output.HOAConsumerExtended;
import rabinizer.ltl.ValuationSet;
import rabinizer.ltl.ValuationSetFactory;

public abstract class Automaton<S extends IState<S>> {

    protected final ValuationSetFactory valuationSetFactory;
    protected final boolean mergingEnabled;

    protected final Set<S> states;
    protected final Table<S, ValuationSet, S> transitions;
    protected final Table<S, S, ValuationSet> edgeBetween;
    protected S initialState;
    protected S trapState;

    protected Automaton(ValuationSetFactory valuationSetFactory) {
        this(valuationSetFactory, true);
    }

    protected Automaton(ValuationSetFactory valuationSetFactory, boolean mergingEnabled) {
        this.valuationSetFactory = valuationSetFactory;
        this.mergingEnabled = mergingEnabled;
        states = new HashSet<>();
        transitions = HashBasedTable.create();
        edgeBetween = HashBasedTable.create();
    }

    protected Automaton(Automaton<S> a) {
        states = a.states;
        transitions = a.transitions;
        initialState = a.initialState;
        edgeBetween = a.edgeBetween;
        valuationSetFactory = a.valuationSetFactory;
        trapState = a.trapState;
        mergingEnabled = a.mergingEnabled;
    }

    public void generate() {
        generate(getInitialState());
    }

    public void generate(S initialState) {
        // Return if already generated
        if (!states.add(initialState)) {
            return;
        }

        Queue<S> workList = new ArrayDeque<>();
        workList.add(initialState);

        while (!workList.isEmpty()) {
            S current = workList.remove();

            Map<ValuationSet, S> successors = current.getSuccessors();
            Map<S, ValuationSet> reverseMap = edgeBetween.row(current);

            // Insert all successors and construct reverse map.
            for (Map.Entry<ValuationSet, S> transition : successors.entrySet()) {
                ValuationSet edge = transition.getKey();
                S successor = transition.getValue();

                ValuationSet vs = reverseMap.remove(successor);

                if (vs == null) {
                    vs = edge.clone();
                    transitions.put(current, edge, successor);
                } else if (mergingEnabled) {
                    transitions.remove(current, vs);
                    vs.addAll(edge);
                    transitions.put(current, vs, successor);
                } else {
                    transitions.put(current, edge, successor);
                    vs.addAll(edge);
                }

                reverseMap.put(successor, vs);

                if (states.add(successor)) {
                    workList.add(successor);
                }
            }
        }
    }

    public boolean isSink(S state) {
        ValuationSet valuationSet = edgeBetween.get(state, state);
        return valuationSet != null && valuationSet.isUniverse();
    }

    public boolean isLooping(S state) {
        ValuationSet valuationSet = edgeBetween.get(state, state);
        return valuationSet != null && !valuationSet.isEmpty();
    }

    public void removeSinks() {
        List<S> sinks = states.stream().filter(this::isSink).collect(Collectors.toList());

        for (S s : sinks) {
            transitions.row(s).clear();
            edgeBetween.row(s).clear();
        }
    }

    public S getSuccessor(S s, Set<String> v) {
        for (ValuationSet vs : transitions.row(s).keySet()) {
            if (vs.contains(v)) {
                return transitions.get(s, vs);
            }
        }

        return null;
    }

    public Map<ValuationSet, S> getSuccessors(S state) {
        generate(state);
        return Collections.unmodifiableMap(transitions.row(state));
    }

    public int size() {
        return states.size();
    }

    public String toDotty() {
        String r = "digraph \"Automaton for " + initialState + "\" \n{\n";

        for (IState s : states) {
            if (s == initialState) {
                r += "node [shape=oval, label=\"" + s + "\"]\"" + s + "\";\n";
            } else {
                r += "node [shape=rectangle, label=\"" + s + "\"]\"" + s + "\";\n";
            }
        }

        for (Table.Cell<S, ValuationSet, S> cell : transitions.cellSet()) {
            r += "\"" + cell.getRowKey() + "\" -> \"" + cell.getColumnKey() + "\" [label=\"" + cell.getValue()
                    + "\"];\n";
        }

        return r + "}";
    }

    /**
     * This method is only there for debugging.
     *
     */
    public void toHOA(OutputStream o) throws HOAConsumerException {
        HOAConsumerExtended hoa = new HOAConsumerExtended(new HOAConsumerPrint(o), false);
        hoa.setHeader(new ArrayList<>(valuationSetFactory.getAlphabet()));
        hoa.setInitialState(this.initialState);
        hoa.setAcceptanceCondition(Collections.emptyList());

        for (S s : states) {
            hoa.addState(s);
            for (Table.Cell<S, ValuationSet, S> trans : transitions.cellSet()) {
                if (trans.getRowKey().equals(s)) {
                    List<Integer> accSets = null;

                    hoa.addEdge(trans.getRowKey(), trans.getColumnKey().toFormula(), trans.getValue(), null);
                }
            }
        }

        hoa.done();
    }

    public String acc() {
        return "";
    }

    public Set<S> getStates() {
        return Collections.unmodifiableSet(states);
    }

    public Table<S, ValuationSet, S> getTransitions() {
        return transitions;
    }

    public S getInitialState() {
        if (initialState == null) {
            initialState = generateInitialState();
        }

        return initialState;
    }

    /**
     * if the automaton is not complete anymore (e.g. because of optimization),
     * this method makes it complete by adding a trap state. If you use it after
     * the generation of the Acceptance-condition, either update the
     * Acceptance-condition or make sure, every generalized RabinPair is not a
     * Tautology (like Fin(emptySet)&Inf(allTransitions))
     */
    void makeComplete() {
        boolean usedTrapState = false;

        if (initialState == null) {
            initialState = trapState;
            usedTrapState = true;
        }

        Map<S, Map<ValuationSet, S>> trans = transitions.rowMap();

        for (S s : states) {
            ValuationSet vs = valuationSetFactory.createEmptyValuationSet();
            Set<Map.Entry<ValuationSet, S>> transOfS;
            if (trans.get(s) != null) {
                transOfS = trans.get(s).entrySet();
            } else {
                transOfS = Collections.emptySet();
            }

            transOfS.stream().forEach(edge -> vs.addAll(edge.getKey()));
            ValuationSet vs2 = vs.complement(); // because vs has to be
            // final or effectively
            // final acc. to compiler
            if (!vs2.isEmpty()) {
                transitions.put(s, vs2, trapState);
                edgeBetween.put(s, trapState, vs2);
                usedTrapState = true;
            }
        }

        if (usedTrapState) {
            transitions.put(trapState, valuationSetFactory.createUniverseValuationSet(), trapState);
            edgeBetween.put(trapState, trapState, valuationSetFactory.createUniverseValuationSet());
            states.add(trapState);
        }
    }

    public List<Set<S>> SCCs() {
        return SCCAnalyser.SCCs(this, this.initialState);
    }

    public List<Set<S>> subSCCs(Set<S> SCC, Map<S, ValuationSet> forbiddenEdges) {
        return SCCAnalyser.subSCCs(this, SCC, forbiddenEdges);
    }

    /**
     * This method removes unused states and their in- and outgoing transitions.
     * If the set dependsOn the initial state, it becomes an automaton with the
     * only state false. Use this method only if you are really sure you want to
     * remove the states! The method is designed for the assumptions, that only
     * nonaccepting SCCs are deleted, and the idea is also that everything,
     * which is deleted will be replaced with a trap state (in makeComplete).
     *
     * @param statess: Set of states that is to be removed
     */
    public void removeStates(Set<S> statess) {
        if (statess.contains(initialState)) {
            states.clear();
            transitions.clear();
            initialState = null;
            edgeBetween.clear();
        } else {
            states.removeAll(statess);

            for (S state : statess) {
                transitions.row(state).clear();
                edgeBetween.row(state).clear();
                edgeBetween.column(state).clear();
            }

            Iterator<Cell<S, ValuationSet, S>> it = transitions.cellSet().iterator();
            while (it.hasNext()) {
                if (statess.contains(it.next().getValue())) {
                    it.remove();
                }
            }
        }
    }

    // TODO to abstract ProductAutomaton ?
    protected Set<ValuationSet> generatePartitioning(Set<Set<ValuationSet>> product) {
        Set<ValuationSet> partitioning = new HashSet<>();
        partitioning.add(valuationSetFactory.createUniverseValuationSet());

        for (Set<ValuationSet> vSets : product) {
            Set<ValuationSet> partitioningNew = new HashSet<>();

            for (ValuationSet vSet : vSets) {
                for (ValuationSet vSetOld : partitioning) {
                    ValuationSet vs = vSetOld.clone();
                    vs.retainAll(vSet);
                    partitioningNew.add(vs);
                }
            }

            partitioning = partitioningNew;
        }

        partitioning.remove(valuationSetFactory.createEmptyValuationSet());
        return partitioning;
    }

    protected abstract S generateInitialState();


    /**
     * @param scc: an SCC for which the transitions inside need to be determined
     * @return all transitions where start is in the SCC
     */

    protected Set<Table.Cell<S, ValuationSet, S>> getTransitionsInSCC(Set<S> scc) {
        Set<Table.Cell<S, ValuationSet, S>> result = new HashSet<>();
        for (Table.Cell<S, ValuationSet, S> entry : transitions.cellSet()) {

            if (scc.contains(entry.getRowKey())) {
                result.add(entry);
            }

        }
        return result;

    }

    /**
     * This method has no side effects
     *
     * @param scc: set of states
     * @return true if the only transitions from scc go to scc again and false
     * otherwise
     */
    protected boolean isSink(Set<S> scc) {
        Set<S> nonSCCStates = new HashSet<>(states);
        nonSCCStates.removeAll(scc);
        return scc.stream().filter(s -> transitions.row(s) != null)
                .allMatch(s -> (Collections.disjoint(transitions.row(s).values(), nonSCCStates)));
    }

}
