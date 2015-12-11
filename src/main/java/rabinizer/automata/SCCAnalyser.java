package rabinizer.automata;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.lang.Math;

import rabinizer.ltl.ValuationSet;

public class SCCAnalyser<State> {
    private int n = 0;
    private Map<State, Integer> lowlink = new HashMap<State, Integer>();
    private Map<State, Integer> number = new HashMap<State, Integer>();
    private Set<State> onStack = new HashSet<State>();
    private Deque<State> stack = new ArrayDeque<State>();
    private Automaton<State> a;
    private Map<State, ValuationSet> forbiddenEdges;
    private Set<State> allowedStates;

    /**
     * This method computes the SCCs of the state-/transition-graph of the
     * automaton. It is based on Tarjan's strongly connected component
     * algorithm. It runs in linear time, assuming the Map-operation get and put
     * and containsKey (and the onStack set-operations) take constant time,
     * which is acc. to java Documentation the case if the hash-function is good
     * enough, also the checks for forbiddenEdges and allowedState need to be
     * constant for the function to run in linear time.
     * 
     * @param a:
     *            Automaton, for which the class is analysed
     * 
     * @return list of set of states, where each set corresponds to a (maximal)
     *         SCC.. The list is ordered according to the topological ordering
     *         in the "condensation graph", aka the graph where the SCCs are
     *         vertices, ordered such that for each transition a->b in the
     *         condensation graph, a is in the list before b
     */
    public static <State> List<Set<State>> SCCs(Automaton<State> a) {
        SCCAnalyser<State> s = new SCCAnalyser<State>(a);
        return s.SCCs();

    }

    /**
     * This method refines the SCC in order to have the sub-SCCs if
     * forbiddenEdges are not allowed to use
     * 
     * @parem a: Automaton, for which the SCC-Analysis has to be made
     * 
     * @param SCC:
     *            the SCC that will be processed
     * @param forbiddenEdges:
     *            the edges that are forbidden
     * @return the sub-SCCs of the SCC as list in topologic ordering
     */
    public static <State> List<Set<State>> subSCCs(Automaton<State> a, Set<State> SCC,
            Map<State, ValuationSet> forbiddenEdges) {
        SCCAnalyser<State> s = new SCCAnalyser<State>(a, SCC, forbiddenEdges);
        return s.subSCCs();

    }

    private SCCAnalyser(Automaton<State> a) {
        this.a = a;
        this.allowedStates = a.states;
        this.forbiddenEdges = Collections.emptyMap();
    }

    private SCCAnalyser(Automaton<State> a, Set<State> s, Map<State, ValuationSet> forbiddenEdges) {
        this.a = a;
        this.allowedStates = s;
        this.forbiddenEdges = forbiddenEdges;

    }

    public List<Set<State>> SCCs() {
        stack.push(a.initialState);
        onStack.add(a.initialState);
        return SCCsRecursively();
    }

    private List<Set<State>> subSCCs() {
        List<Set<State>> result = new ArrayList<Set<State>>();
        Set<State> notYetProcessed = new HashSet<>(allowedStates);
        while (!notYetProcessed.isEmpty()) {

            State state = notYetProcessed.iterator().next();
            stack.push(state);
            onStack.add(state);
            result.addAll(SCCsRecursively());

            for (Set<State> res : result) {
                notYetProcessed.removeAll(res);
            }
        }
        return result;
    }

    private List<Set<State>> SCCsRecursively() {
        n++;
        State v = stack.peek();
        lowlink.put(v, n);
        number.put(v, n);
        Map<ValuationSet, State> trans = a.transitions.row(v);
        List<Set<State>> result = new ArrayList<Set<State>>();
        for (ValuationSet vs : trans.keySet()) {

            State w = trans.get(vs);
            if (!allowedStates.contains(w)) {
                continue;
            } else if (!number.containsKey(w) && forbiddenEdges.get(v) != vs) {
                stack.push(w);
                onStack.add(w);
                result.addAll(SCCsRecursively());
                lowlink.put(v, Math.min(lowlink.get(v), lowlink.get(w)));
            } else if (number.get(w) < number.get(v) && onStack.contains(w)) {
                lowlink.put(v, Math.min(lowlink.get(v), number.get(w)));
            }

        }
        if (lowlink.get(v) == number.get(v)) {
            Set<State> set = new HashSet<State>();
            while (!stack.isEmpty()) {
                if (number.get(stack.peek()) >= number.get(v)) {
                    State w = stack.pop();
                    onStack.remove(w);
                    set.add(w);
                } else {
                    break;
                }
            }
            result.add(set);
        }
        return result;
    }

}
