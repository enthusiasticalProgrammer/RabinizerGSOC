package rabinizer.automata;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

import rabinizer.exec.Main;
import rabinizer.ltl.ValuationSet;
import rabinizer.ltl.ValuationSetFactory;

import java.util.*;

/**
 * @param <State>
 * @author jkretinsky
 */
public abstract class Automaton<State> {

    protected Set<State> states;
    protected Set<State> sinks;
    protected Table<State, ValuationSet, State> transitions;
    protected Table<State, State, ValuationSet> edgeBetween;
    protected State initialState;

    protected final ValuationSetFactory<String> valuationSetFactory;

    protected Automaton(ValuationSetFactory<String> valuationSetFactory) {
        states = new HashSet<>();
        sinks = new HashSet<>();

        transitions = HashBasedTable.create();
        edgeBetween = HashBasedTable.create();

        this.valuationSetFactory = valuationSetFactory;
    }

    protected Automaton(Automaton<State> a) {
        states = a.states;
        transitions = a.transitions;
        initialState = a.initialState;
        sinks = a.sinks;
        edgeBetween = a.edgeBetween;
        this.valuationSetFactory = a.valuationSetFactory;
    }

    // TODO to abstract ProductAutomaton ?
    protected Set<ValuationSet> generatePartitioning(Set<Set<ValuationSet>> product) {
        Set<ValuationSet> partitioning = new HashSet<>();
        partitioning.add(valuationSetFactory.createUniverseValuationSet());
        for (Set<ValuationSet> vSets : product) {
            Set<ValuationSet> partitioningNew = new HashSet<>();

            for (ValuationSet vSet : vSets) {
                for (ValuationSet vSetOld : partitioning) {
                    ValuationSet vs = valuationSetFactory.createValuationSet(vSetOld);
                    vs.retainAll(vSet);
                    partitioningNew.add(vs);
                }
            }

            partitioning = partitioningNew;
        }

        partitioning.remove(valuationSetFactory.createEmptyValuationSet());
        return partitioning;
    }

    protected abstract State generateInitialState();

    protected abstract State generateSuccState(State s, ValuationSet vs);

    protected abstract Set<ValuationSet> generateSuccTransitions(State s);

    public void generate() {
        initialState = generateInitialState();
        states.add(initialState);

        // TODO: Move this to a statistics class
        Main.nonsilent("  Generating automaton for " + initialState);

        Queue<State> workList = new ArrayDeque<>();
        workList.add(initialState);

        while (!workList.isEmpty()) {
            State curr = workList.remove();

            Main.verboseln("\tCurrState: " + curr);
            Set<ValuationSet> succValSets = generateSuccTransitions(curr);
            Main.verboseln("\t  CurrTrans: " + succValSets);

            Map<State, ValuationSet> succMapping = new HashMap<>();

            // Construct all successor states
            for (ValuationSet succVals : succValSets) {
                // TODO: fix generateSuccTransitions
                if (succVals.isEmpty()) {
                    continue;
                }

                State succ = generateSuccState(curr, succVals);
                Main.verboseln("\t  SuccState: " + succ);

                // Combine with existing transition
                ValuationSet vs = succMapping.remove(succ);

                if (vs != null) {
                    vs.addAll(succVals);
                } else {
                    vs = succVals;
                }

                succMapping.put(succ, vs);
            }

            // Register all outgoing transitions
            for (Map.Entry<State, ValuationSet> entry : succMapping.entrySet()) {
                State succ = entry.getKey();
                ValuationSet vs = entry.getValue();

                transitions.put(curr, vs, succ);
                edgeBetween.put(curr, succ, vs);

                if (!states.contains(succ)) {
                    states.add(succ);
                    workList.add(succ);
                }
            }

            // Mark as a sink
            if (edgeBetween.contains(curr, curr) && edgeBetween.get(curr, curr).isUniverse()) {
                sinks.add(curr);
            }
        }

        Main.nonsilent("  Number of states: " + states.size());
    }

    public void removeSinks() {
        for (State s : sinks) {
            transitions.row(s).clear();
            edgeBetween.row(s).clear();
        }
    }

    public State succ(State s, Set<String> v) {
        for (ValuationSet vs : transitions.row(s).keySet()) {
            if (vs.contains(v)) {
                return transitions.get(s, vs);
            }
        }

        return null;
    }

    public int size() {
        return states.size();
    }

    public String toDotty() {
        String r = "digraph \"Automaton for " + initialState + "\" \n{\n";

        for (State s : states) {
            if (s == initialState) {
                r += "node [shape=oval, label=\"" + s + "\"]\"" + s + "\";\n";
            } else {
                r += "node [shape=rectangle, label=\"" + s + "\"]\"" + s + "\";\n";
            }
        }

        for (Table.Cell<State, ValuationSet, State> cell : transitions.cellSet()) {
            r += "\"" + cell.getRowKey() + "\" -> \"" + cell.getColumnKey() + "\" [label=\"" + cell.getValue()
                    + "\"];\n";
        }

        return r + "}";
    }

    protected static <K> int getId(Map<K, Integer> map, K key) {
        Integer r = map.get(key);

        if (r == null) {
            int id = map.size();
            map.put(key, id);
            return id;
        }

        return r;
    }

    public String toHOA() {
        Map<State, Integer> statesToNumbers = new HashMap<>();

        statesToNumbers.put(initialState, 0);

        String dot = "";
        dot += "HOA: v1\n";
        dot += "tool: \"Rabinizer\" \"3.1\"\n";
        dot += "name: \"Automaton for " + initialState + "\"\n";
        dot += "properties: deterministic\n";
        dot += "properties: complete\n";
        dot += "States: " + states.size() + "\n";
        dot += "Start: " + statesToNumbers.get(initialState) + "\n";
        dot += accName();
        dot += "Acceptance: " + accTypeNumerical() + "\n"; // TODO: handle
        dot += "AP: " + valuationSetFactory.getAlphabet().size();

        // trivial sets
        for (String letter : valuationSetFactory.getAlphabet()) {
            dot += " \"" + letter + "\"";
        }
        dot += "\n";
        dot += "--BODY--\n";

        for (State s : this.states) {
            getId(statesToNumbers, s);
            dot += "State: " + statesToNumbers.get(s) + " \"" + s + "\" " + stateAcc(s) + "\n";
            dot += outTransToHOA(s, statesToNumbers);
        }

        return dot + "--END--\n";
    }

    public String acc() {
        return "";
    }

    public Set<State> getStates() {
        return states;
    }

    public Table<State, ValuationSet, State> getTransitions() {
        return transitions;
    }

    public State getInitialState() {
        return initialState;
    }

    protected String accName() {
        return "";
    }

    protected String accTypeNumerical() {
        return "";
    }

    protected String stateAcc(State s) {
        return "";
    }

    protected String outTransToHOA(State s, Map<State, Integer> statesToNumbers) {
        String result = "";
        for (Map.Entry<ValuationSet, State> entry : transitions.row(s).entrySet()) {
            result += "[" + entry.getKey().toFormula() + "] " + statesToNumbers.get(entry.getValue()) + "\n";
        }
        return result;
    }

    /**
     * This method removes unused states and their in- and outgoing transitions.
     * If the set contains the initial state, it becomes an automaton with the
     * only state false. Use this method only if you are really sure you want to
     * remove the states! The method is designed for the assumptions, that only
     * nonaccepting SCCs are deleted, and the idea is also that everything,
     * which is deleted will be replaced with a trap state (in makeComplete).
     * 
     * @param statess:
     *            Set of states that is to be removed
     */
    protected void removeStates(Set<State> statess) {
        if (statess.contains(initialState)) {
            states = Collections.emptySet();
            transitions = HashBasedTable.create();
            initialState = null;
            sinks = Collections.emptySet();
            edgeBetween = HashBasedTable.create();
        } else {
            states.removeAll(statess);
            sinks.removeAll(statess);

            // fix transitions
            Cell<State, ValuationSet, State> entry = null;
            for (Iterator<Cell<State, ValuationSet, State>> it = transitions.cellSet().iterator(); it
                    .hasNext(); entry = it.next()) {

                if (statess.contains(entry.getRowKey())) {
                    it.remove();
                } else if (statess.contains(entry.getValue())) {
                    it.remove();
                }
            }

            // fix edgeBetwwen
            Cell<State, State, ValuationSet> entry2 = null;
            for (Iterator<Cell<State, State, ValuationSet>> it = edgeBetween.cellSet().iterator(); it
                    .hasNext(); entry2 = it.next()) {
                if (statess.contains(entry2.getRowKey())) {
                    it.remove();
                } else if (statess.contains(entry2.getColumnKey())) {
                    it.remove();
                }
            }
        }
    }

    /**
     * 
     * @param scc:
     *            an SCC for which the transitions inside need to be determined
     * @return all transitions where start is in the SCC
     */

    protected Set<Table.Cell<State, ValuationSet, State>> getTransitionsInSCC(Set<State> scc) {
        Set<Table.Cell<State, ValuationSet, State>> result = new HashSet<Table.Cell<State, ValuationSet, State>>();
        for (Table.Cell<State, ValuationSet, State> entry : transitions.cellSet()) {

            if (scc.contains(entry.getRowKey())) {
                result.add(entry);
            }

        }
        return result;

    }

    /**
     * This method has no side effects
     * 
     * @param scc:
     *            set of states
     * @return true if the only transitions from scc go to scc again and false
     *         otherwise
     */
    protected boolean isSink(Set<State> scc) {
        Set<State> nonSCCStates = new HashSet<>(states);
        nonSCCStates.removeAll(scc);
        return scc.stream().filter(s -> transitions.row(s) != null)
                .allMatch(s -> (Collections.disjoint(transitions.row(s).values(), nonSCCStates)));
    }

    /**
     * if the automaton is not complete anymore (e.g. because of optimization),
     * this method makes it complete by adding a trap state.
     */
    public void makeComplete() {
        throw new RuntimeException("Not yet implemented");
    }

    public List<Set<State>> SCCs() {
        return SCCAnalyser.<State> SCCs(this);
    }

    public List<Set<State>> subSCCs(Set<State> SCC, Map<State, ValuationSet> forbiddenEdges) {
        return SCCAnalyser.<State> subSCCs(this, SCC, forbiddenEdges);
    }
}
