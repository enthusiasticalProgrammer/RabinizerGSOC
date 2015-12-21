package rabinizer.automata;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import rabinizer.ltl.ValuationSet;
import rabinizer.ltl.ValuationSetFactory;

import java.util.*;

public abstract class Automaton<S extends IState<S>> {

    protected final ValuationSetFactory<String> valuationSetFactory;

    protected final Set<S> states;
    protected final Set<S> sinks;
    protected final Table<S, ValuationSet, S> transitions;
    protected final Table<S, S, ValuationSet> edgeBetween;
    protected S initialState;
    protected S trapState;

    protected Automaton(ValuationSetFactory<String> valuationSetFactory) {
        states = new HashSet<>();
        sinks = new HashSet<>();

        transitions = HashBasedTable.create();
        edgeBetween = HashBasedTable.create();

        this.valuationSetFactory = valuationSetFactory;
        trapState = null;
    }

    protected Automaton(Automaton<S> a) {
        states = a.states;
        transitions = a.transitions;
        initialState = a.initialState;
        sinks = a.sinks;
        edgeBetween = a.edgeBetween;
        this.valuationSetFactory = a.valuationSetFactory;
        trapState = a.trapState;
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

    public void generate() {
        generate(getInitialState());
    }

    public void generate(S initialState) {
        states.add(initialState);

        // TODO: Move this to a statistics class
        // Main.nonsilent(" Generating transitions for " + initialState);

        Queue<S> workList = new ArrayDeque<>();
        workList.add(initialState);

        while (!workList.isEmpty()) {
            S curr = workList.remove();

            // Main.verboseln("\tCurrState: " + curr);
            Set<ValuationSet> succValSets = generateSuccTransitions(curr);
            // Main.verboseln("\t CurrTrans: " + succValSets);

            Map<S, ValuationSet> succMapping = new HashMap<>();

            // Construct all successor states
            for (ValuationSet succVals : succValSets) {
                // TODO: fix generateSuccTransitions
                if (succVals.isEmpty()) {
                    continue;
                }

                S succ = curr.getSuccessor(succVals.pickAny());
                // Main.verboseln("\t SuccState: " + succ);

                if (succ != null) {
                    // Combine with existing transition
                    ValuationSet vs = succMapping.remove(succ);

                    if (vs != null) {
                        vs.addAll(succVals);
                    } else {
                        vs = succVals;
                    }

                    succMapping.put(succ, vs);
                }
            }

            // Register all outgoing transitions
            for (Map.Entry<S, ValuationSet> entry : succMapping.entrySet()) {
                S succ = entry.getKey();
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

        // Main.nonsilent(" Number of states: " + states.size());
    }

    public void removeSinks() {
        for (S s : sinks) {
            transitions.row(s).clear();
            edgeBetween.row(s).clear();
        }
    }

    public S succ(S s, Set<String> v) {
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

    public String toHOA() {
        Map<S, Integer> statesToNumbers = new HashMap<>();

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

        for (S s : this.states) {
            getId(statesToNumbers, s);
            dot += "State: " + statesToNumbers.get(s) + " \"" + s + "\" " + stateAcc(s) + "\n";
            dot += outTransToHOA(s, statesToNumbers);
        }

        return dot + "--END--\n";
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
     * this method makes it complete by adding a trap state.
     */
    public void makeComplete() {
        boolean usedTrapState = false;
        if (initialState == null) {
            initialState = trapState;
            sinks.add(trapState);
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
                sinks.add(trapState);
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
        return SCCAnalyser.SCCs(this);
    }

    public List<Set<S>> subSCCs(Set<S> SCC, Map<S, ValuationSet> forbiddenEdges) {
        return SCCAnalyser.subSCCs(this, SCC, forbiddenEdges);
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

    protected abstract S generateInitialState();

    protected Set<ValuationSet> generateSuccTransitions(IState s) {
        return s.partitionSuccessors();
    }

    protected String accName() {
        return "";
    }

    protected String accTypeNumerical() {
        return "";
    }

    protected String stateAcc(S s) {
        return "";
    }

    protected String outTransToHOA(S s, Map<S, Integer> statesToNumbers) {
        String result = "";
        for (Map.Entry<ValuationSet, S> entry : transitions.row(s).entrySet()) {
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
    public void removeStates(Set<S> statess) {
        if (statess.contains(initialState)) {
            states.clear();
            transitions.clear();
            initialState = null;
            sinks.clear();
            edgeBetween.clear();
        } else {
            states.removeAll(statess);
            sinks.removeAll(statess);

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

    /**
     * @param scc:
     *            an SCC for which the transitions inside need to be determined
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
     * @param scc:
     *            set of states
     * @return true if the only transitions from scc go to scc again and false
     *         otherwise
     */
    protected boolean isSink(Set<S> scc) {
        Set<S> nonSCCStates = new HashSet<>(states);
        nonSCCStates.removeAll(scc);
        return scc.stream().filter(s -> transitions.row(s) != null)
                .allMatch(s -> (Collections.disjoint(transitions.row(s).values(), nonSCCStates)));
    }

}
