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

package rabinizer.automata;

import rabinizer.collections.Collections3;
import rabinizer.collections.TarjanStack;
import rabinizer.collections.valuationset.ValuationSet;

import java.util.*;

/**
 * @author Christopher Ziegler
 */
public class SCCAnalyser<S extends IState<S>> {
    private final Map<S, Integer> lowlink = new HashMap<>();
    private final Map<S, Integer> number = new HashMap<>();
    private final Deque<S> stack = new TarjanStack<>();
    private final Automaton<S> a;
    private final TranSet<S> forbiddenEdges;
    private final Set<S> allowedStates;
    private int n = 0;

    private SCCAnalyser(Automaton<S> a) {
        this(a, a.states, new TranSet<>(a.valuationSetFactory));
    }

    private SCCAnalyser(Automaton<S> a, Set<S> s, TranSet<S> forbiddenEdges) {
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
    public static <S extends IState<S>> List<TranSet<S>> SCCs(Automaton<S> a, S initialState) {
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
     * @param a: Automaton, for which the SCC-Analysis has to be made
     */
    public static <S extends IState<S>> List<TranSet<S>> subSCCs(Automaton<S> a, Set<S> SCC, TranSet<S> forbiddenEdges) {
        SCCAnalyser<S> s = new SCCAnalyser<>(a, SCC, forbiddenEdges);
        return s.subSCCs();
    }

    /**
     * This method refines the SCC in order to have the sub-SCCs if
     * forbiddenEdges are not allowed to use
     *
     * @param SCC:            the SCC that will be processed
     * @param forbiddenEdges: the edges that are forbidden
     * @return the sub-SCCs of the SCC as list in topologic ordering
     * @param a: Automaton, for which the SCC-Analysis has to be made
     */
    public static <S extends IState<S>> List<TranSet<S>> subSCCs(Automaton<S> a, TranSet<S> SCC, TranSet<S> forbiddenEdges) {
        SCCAnalyser<S> s = new SCCAnalyser<>(a, SCC.asMap().keySet(), forbiddenEdges);
        return s.subSCCs();
    }

    public List<TranSet<S>> SCCs() {
        stack.push(a.initialState);
        return SCCsRecursively();
    }

    private List<TranSet<S>> subSCCs() {
        List<TranSet<S>> result = new ArrayList<>();
        Set<S> notYetProcessed = new HashSet<>(allowedStates);

        while (!notYetProcessed.isEmpty()) {
            S state = Collections3.removeElement(notYetProcessed);
            stack.push(state);
            result.addAll(SCCsRecursively());
            result.forEach(s -> notYetProcessed.removeAll(s.asMap().keySet()));
        }

        return result;
    }

    private List<TranSet<S>> SCCsRecursively() {
        n++;
        S v = stack.peek();
        lowlink.put(v, n);
        number.put(v, n);
        List<TranSet<S>> result = new ArrayList<>();

        for (Map.Entry<ValuationSet, S> entry : a.transitions.row(v).entrySet()) {
            // edge not forbidden
            if (!forbiddenEdges.containsAll(v, entry.getKey())) {
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

            TranSet<S> tranSet = new TranSet<>(a.valuationSetFactory);

            for (S s : set) {
                for (Map.Entry<ValuationSet, S> entry : a.transitions.row(s).entrySet()) {
                    if (set.contains(entry.getValue()) && !forbiddenEdges.containsAll(s, entry.getKey())) {
                        tranSet.addAll(s, entry.getKey());
                    }
                }
            }

            result.add(tranSet);
        }

        return result;
    }

}
