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

import com.google.common.collect.Sets;
import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import rabinizer.collections.valuationset.ValuationSet;
import rabinizer.collections.valuationset.ValuationSetFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

public abstract class Automaton<S extends IState<S>> {

    protected final ValuationSetFactory valuationSetFactory;
    protected final Set<S> states;
    protected final Map<S, Map<S, ValuationSet>> transitions;
    @Nullable protected S initialState;

    protected Automaton(Automaton<S> a) {
        valuationSetFactory = a.valuationSetFactory;
        states = a.states;
        transitions = a.transitions;
        initialState = a.initialState;
    }

    protected Automaton(ValuationSetFactory valuationSetFactory) {
        this.valuationSetFactory = valuationSetFactory;
        states = new HashSet<>();
        transitions = new HashMap<>();
    }

    public void generate() {
        generate(getInitialState());
    }

    public void generate(S initialState) {
        // Return if already generated
        if (states.contains(initialState)) {
            return;
        }

        Queue<S> workList = new ArrayDeque<>();
        workList.add(initialState);

        while (!workList.isEmpty()) {
            S current = workList.remove();
            Collection<S> next = generateSingleState(current);

            for (S successor : next) {
                if (!states.contains(successor)) {
                    workList.add(successor);
                }
            }
        }
    }

    public boolean hasSuccessors(S state) {
        return !row(transitions, state).isEmpty();
    }

    public boolean isSink(S state) {
        ValuationSet valuationSet = get(transitions, state, state);
        return valuationSet != null && valuationSet.isUniverse();
    }

    public boolean isLooping(S state) {
        ValuationSet valuationSet = get(transitions, state, state);
        return valuationSet != null && !valuationSet.isEmpty();
    }

    @Nullable
    public S getSuccessor(S state, Set<String> valuation) {
        for (Map.Entry<S, ValuationSet> transition : getSuccessors(state).entrySet()) {
            if (transition.getValue().contains(valuation)) {
                return transition.getKey();
            }
        }

        return null;
    }

    public Map<S, ValuationSet> getSuccessors(S state) {
        generateSingleState(state);
        return transitions.get(state);
    }

    public int size() {
        return states.size();
    }

    public Set<S> getStates() {
        return Collections.unmodifiableSet(states);
    }

    public S getInitialState() {
        if (initialState == null) {
            initialState = generateInitialState();
        }

        return initialState;
    }

    public Set<S> getReachableStates() {
        HashSet<S> reach = new HashSet<>();
        reach.add(getInitialState());
        getReachableStates(reach);
        return reach;
    }

    public void getReachableStates(Set<S> states) {
        Deque<S> workList = new ArrayDeque<>(states);

        while (!workList.isEmpty()) {
            S state = workList.remove();

            transitions.get(state).forEach((suc, v) -> {
                if (states.add(suc)) {
                    workList.add(suc);
                }
            });
        }
    }

    public void removeUnreachableStates() {
        removeUnreachableStates(getReachableStates());
    }

    public void removeUnreachableStates(Set<S> reach) {
        getReachableStates(reach);
        removeStatesIf(s -> !reach.contains(s));
    }

    public List<TranSet<S>> SCCs() {
        return SCCAnalyser.SCCs(this, initialState);
    }

    public List<TranSet<S>> subSCCs(TranSet<S> SCC, TranSet<S> forbiddenEdges) {
        return SCCAnalyser.subSCCs(this, SCC, forbiddenEdges);
    }

    public void toHOA(HOAConsumer hoa) throws HOAConsumerException {
        throw new UnsupportedOperationException();
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
    public void removeStates(Collection<S> statess) {
        if (statess.contains(initialState)) {
            states.clear();
            transitions.clear();
            initialState = null;
        } else {
            removeStatesIf(statess::contains);
        }
    }

    public void removeStatesIf(Predicate<S> predicate) {
        Iterator<S> iterator = states.iterator();

        while (iterator.hasNext()) {
            S state = iterator.next();

            if (!predicate.test(state)) {
                continue;
            }

            iterator.remove();
            transitions.remove(state);
            transitions.forEach((k, v) -> v.remove(state));
        }

        if (predicate.test(initialState)) {
            initialState = null;
        }
    }

    public Collection<String> getAlphabet() {
        return valuationSetFactory.getAlphabet();
    }

    protected S generateInitialState() {
        throw new UnsupportedOperationException();
    }

    /**
     * This method has no side effects
     *
     * @param scc: set of states
     * @return true if the only transitions from scc go to scc again and false
     * otherwise
     */
    protected boolean isSink(Set<S> scc) {
        Set<S> nonSCCStates = Sets.difference(states, scc);
        return scc.stream().allMatch(s -> (Collections.disjoint(row(transitions, s).keySet(), nonSCCStates)));
    }

    /**
     * The method replaces antecessor by replacement. Both must be in the
     * states-set (and both must not be null) when calling the method.
     * Antecessor gets deleted during the method, and the transitions to
     * antecessor will be recurved towards replacement.
     * <p>
     * The method throws an IllegalArgumentException, when one of the parameters
     * is not in the states-set
     */
    protected void replaceBy(S antecessor, S replacement) {
        if (!(states.contains(antecessor) && states.contains(replacement))) {
            throw new IllegalArgumentException();
        }

        states.remove(antecessor);
        transitions.get(antecessor).clear();

        for (Map<S, ValuationSet> edges : transitions.values()) {
            ValuationSet vs = edges.get(antecessor);

            if (vs == null) {
                continue;
            }

            ValuationSet vs2 = edges.get(replacement);

            if (vs2 == null) {
                edges.put(replacement, vs);
            } else {
                vs2.addAll(vs);
            }
        }

        if (antecessor.equals(initialState)) {
            initialState = replacement;
        }
    }

    @Nonnull
    private Set<S> generateSingleState(S state) {
        if (states.add(state)) {
            Map<ValuationSet, S> successors = state.getSuccessors();
            Map<S, ValuationSet> row = row(transitions, state);

            // Insert all successors into row.
            successors.forEach((valuation, successor) -> {
                ValuationSet vs = row.get(successor);

                if (vs == null) {
                    row.put(successor, valuation.clone());
                } else {
                    vs.addAll(valuation);
                }
            });

            return row.keySet();
        }

        return Collections.emptySet();
    }

    @Nonnull
    private static <R, C, V> Map<C, V> row(Map<R, Map<C, V>> table, R rowKey) {
        Map<C, V> row = table.get(rowKey);

        if (row == null) {
            row = new LinkedHashMap<>();
            table.put(rowKey, row);
        }

        return row;
    }

    @Nullable
    private static <R, C, V> V get(Map<R, Map<C, V>> table, R rowKey, C columnKey) {
        Map<C, V> row = table.get(rowKey);

        if (row == null) {
            return null;
        }

        return row.get(columnKey);
    }
}
