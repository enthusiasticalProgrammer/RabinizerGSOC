package rabinizer.automata;

import rabinizer.collections.HashTarjanStack;
import rabinizer.collections.TarjanStack;
import rabinizer.ltl.ValuationSet;

import java.util.*;

/**
 * @author Christopher Ziegler
 */
public class SCCAnalyser<S extends IState<S>> {
    private final Map<S, Integer> lowlink = new HashMap<>();
    private final Map<S, Integer> number = new HashMap<>();
    private final TarjanStack<S> stack = new HashTarjanStack<>();
    private final Automaton<S> a;
    private final Map<S, ValuationSet> forbiddenEdges;
    private final Set<S> allowedStates;
    private int n = 0;

    private SCCAnalyser(Automaton<S> a) {
        this(a, a.states, Collections.emptyMap());
    }

    private SCCAnalyser(Automaton<S> a, Set<S> s, Map<S, ValuationSet> forbiddenEdges) {
        this.a = a;
        this.allowedStates = s;
        this.forbiddenEdges = forbiddenEdges;
    }

    /**
     * This method computes the SCCs of the state-/transition-graph of the
     * automaton. It is based on Tarjan's strongly connected component
     * algorithm. It runs in linear time, assuming the Map-operation get and put
     * and containsKey (and the onStack set-operations) take constant time,
     * which is acc. to java Documentation the case if the hash-function is good
     * enough, also the checks for forbiddenEdges and allowedState need to be
     * constant for the function to run in linear time.
     *
     * @param a: Automaton, for which the class is analysed
     * @return list of set of states, where each set corresponds to a (maximal)
     * SCC.. The list is ordered according to the topological ordering
     * in the "condensation graph", aka the graph where the SCCs are
     * vertices, ordered such that for each transition a->b in the
     * condensation graph, a is in the list before b
     */
    public static <S extends IState<S>> List<Set<S>> SCCs(Automaton<S> a, S initialState) {
        SCCAnalyser<S> s = new SCCAnalyser<>(a);
        s.stack.push(initialState);
        return s.SCCsRecursively();
    }

    /**
     * This method refines the SCC in order to have the sub-SCCs if
     * forbiddenEdges are not allowed to use
     *
     * @param SCC:            the SCC that will be processed
     * @param forbiddenEdges: the edges that are forbidden
     * @return the sub-SCCs of the SCC as list in topologic ordering
     * @parem a: Automaton, for which the SCC-Analysis has to be made
     */
    public static <S extends IState<S>> List<Set<S>> subSCCs(Automaton<S> a, Set<S> SCC,
                                                             Map<S, ValuationSet> forbiddenEdges) {
        SCCAnalyser<S> s = new SCCAnalyser<>(a, SCC, forbiddenEdges);
        return s.subSCCs();

    }

    public List<Set<S>> SCCs() {
        stack.push(a.initialState);
        return SCCsRecursively();
    }

    private List<Set<S>> subSCCs() {
        List<Set<S>> result = new ArrayList<>();
        Set<S> notYetProcessed = new HashSet<>(allowedStates);
        while (!notYetProcessed.isEmpty()) {

            S state = notYetProcessed.iterator().next();
            stack.push(state);
            result.addAll(SCCsRecursively());

            result.stream().forEach(notYetProcessed::removeAll);
        }
        return result;
    }

    private List<Set<S>> SCCsRecursively() {
        n++;
        S v = stack.peek();
        lowlink.put(v, n);
        number.put(v, n);
        Map<ValuationSet, S> trans = a.transitions.row(v);
        List<Set<S>> result = new ArrayList<>();

        for (Map.Entry<ValuationSet, S> entry : trans.entrySet()) {

            if (!Objects.equals(forbiddenEdges.get(v), entry.getKey())) {// edge
                // not
                // forbidden

                S w = entry.getValue();
                if (allowedStates.contains(w) && !number.containsKey(w)) {
                    stack.push(w);
                    result.addAll(SCCsRecursively());
                    lowlink.put(v, Math.min(lowlink.get(v), lowlink.get(w)));
                } else if (allowedStates.contains(w) && number.get(w) < number.get(v) && stack.contains(w)) {
                    lowlink.put(v, Math.min(lowlink.get(v), number.get(w)));
                }
            }

        }

        if (lowlink.get(v).equals(number.get(v))) {
            Set<S> set = new HashSet<>();
            while (!stack.isEmpty() && number.get(stack.peek()) >= number.get(v)) {
                S w = stack.pop();
                set.add(w);
            }
            result.add(set);
        }

        return result;
    }

}
