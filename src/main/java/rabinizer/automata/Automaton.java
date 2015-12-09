package rabinizer.automata;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import rabinizer.exec.Main;
import rabinizer.ltl.ValuationSet;
import rabinizer.ltl.ValuationSetFactory;

import java.util.*;

/**
 * @param <State>
 * @author jkretinsky
 */
public abstract class Automaton<State> /*implements AccAutomatonInterface*/ {

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
            /*if (finalStates.contains(s)) {
             r += "node [shape=Msquare, label=\"" + displayLabels.get(s) + "\"]\"" + displayLabels.get(s) + "\";\n";
             } else*/
            if (s == initialState) {
                r += "node [shape=oval, label=\"" + s + "\"]\"" + s + "\";\n";
            } else {
                r += "node [shape=rectangle, label=\"" + s + "\"]\"" + s + "\";\n";
            }
        }

        for (Table.Cell<State, ValuationSet, State> cell : transitions.cellSet()) {
            r += "\"" + cell.getRowKey() + "\" -> \"" + cell.getColumnKey() + "\" [label=\"" + cell.getValue() + "\"];\n";
        }

        return r + "}";
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
        dot += "Acceptance: " + accTypeNumerical() + "\n"; //TODO: handle trivial sets
        dot += "AP: " + valuationSetFactory.getAlphabet().size();
        for (String letter : valuationSetFactory.getAlphabet()) {
            dot += " \"" + letter + "\"";
        }
        dot += "\n";
        dot += "--BODY--\n";

        // Map<Tuple<Formula, KState>, Tuple<Formula, KState>> normalStates = new HashMap<Tuple<Formula, KState>, Tuple<Formula, KState>>();        
        for (State s : states) {
            statesToNumbers.put(s, statesToNumbers.size());
            dot += "State: " + statesToNumbers.get(s) + " \"" + s + "\" " + stateAcc(s) + "\n";
            dot += outTransToHOA(s, statesToNumbers);
        }

        return dot + "--END--\n";
    }

    public String acc() {
        return "";
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
}
