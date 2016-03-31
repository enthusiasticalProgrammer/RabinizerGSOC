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

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rabinizer.collections.valuationset.ValuationSet;
import rabinizer.collections.valuationset.ValuationSetFactory;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class Automaton<S extends IState<S>> {

    protected final ValuationSetFactory valuationSetFactory;
    protected final boolean mergingEnabled;

    protected final Set<S> states;
    protected final Table<@NotNull S, @NotNull ValuationSet, @NotNull S> transitions;
    protected final Table<S, S, ValuationSet> edgeBetween;
    protected @Nullable S initialState;

    protected Automaton(ValuationSetFactory valuationSetFactory) {
        this(valuationSetFactory, true);
    }

    protected Automaton(Automaton<S> a) {
        valuationSetFactory = a.valuationSetFactory;
        mergingEnabled = a.mergingEnabled;
        states = a.states;
        transitions = a.transitions;
        edgeBetween = a.edgeBetween;
        initialState = a.initialState;
    }

    protected Automaton(ValuationSetFactory valuationSetFactory, boolean mergingEnabled) {
        this.valuationSetFactory = valuationSetFactory;
        this.mergingEnabled = mergingEnabled;
        states = new HashSet<>();
        transitions = HashBasedTable.create();
        edgeBetween = HashBasedTable.create();
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
        return transitions.row(state).values().stream().anyMatch(s -> !s.equals(state));
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

    public @Nullable S getSuccessor(@NotNull S s, @NotNull Set<String> v) {
        for (Entry<ValuationSet, S> entry : getSuccessors(s).entrySet()) {
            if (entry.getKey().contains(v)) {
                return entry.getValue();
            }
        }

        return null;
    }

    public @NotNull Map<ValuationSet, S> getSuccessors(@NotNull S state) {
        generateSingleState(state);
        return transitions.row(state);
    }

    public int size() {
        return states.size();
    }

    public @NotNull Set<S> getStates() {
        return Collections.unmodifiableSet(states);
    }

    public Table<S, ValuationSet, S> getTransitions() {
        return transitions;
    }

    public @NotNull S getInitialState() {
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

            transitions.row(state).forEach((vs, successor) -> {
                if (states.add(successor)) {
                    workList.add(successor);
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
    public void removeStates(@NotNull Collection<S> statess) {
        if (statess.contains(initialState)) {
            states.clear();
            transitions.clear();
            initialState = null;
            edgeBetween.clear();
        } else {
            removeStatesIf(statess::contains);
        }
    }

    public void removeStatesIf(@NotNull Predicate<S> predicate) {
        Iterator<S> iterator = states.iterator();

        while (iterator.hasNext()) {
            S state = iterator.next();

            if (!predicate.test(state)) {
                continue;
            }

            iterator.remove();
            transitions.row(state).clear();
            edgeBetween.row(state).clear();
            edgeBetween.column(state).clear();
        }

        if (predicate.test(initialState)) {
            initialState = null;
        }

        transitions.values().removeIf(predicate);
    }

    public @NotNull Set<String> getAlphabet() {
        return valuationSetFactory.getAlphabet();
    }

    protected @NotNull S generateInitialState() {
        throw new UnsupportedOperationException();
    }

    /**
     * This method has no side effects
     *
     * @param scc: set of states
     * @return true if the only transitions from scc go to scc again and false
     * otherwise
     */
    protected boolean isSink(@NotNull Set<S> scc) {
        Set<S> nonSCCStates = Sets.difference(states, scc);
        return scc.stream().filter(s -> transitions.row(s) != null)
                .allMatch(s -> (Collections.disjoint(transitions.row(s).values(), nonSCCStates)));
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
    protected void replaceBy(@NotNull S antecessor, @NotNull S replacement) {
        if (!(states.contains(antecessor) && states.contains(replacement))) {
            throw new IllegalArgumentException();
        }

        states.remove(antecessor);
        transitions.row(antecessor).clear();

        Iterator<Table.Cell<S, ValuationSet, S>> it = transitions.cellSet().iterator();

        Table<S, ValuationSet, S> toAdd = HashBasedTable.create();
        while (it.hasNext()) {
            Table.Cell<S, ValuationSet, S> elem = it.next();
            if (antecessor.equals(elem.getValue())) {
                toAdd.put(elem.getRowKey(), elem.getColumnKey(), replacement);
                it.remove();
            }
        }
        transitions.putAll(toAdd);

        edgeBetween.row(antecessor).clear();

        Iterator<Table.Cell<S, S, ValuationSet>> it2 = edgeBetween.cellSet().iterator();

        Table<S, S, ValuationSet> toAdd2 = HashBasedTable.create();
        while (it2.hasNext()) {
            Table.Cell<S, S, ValuationSet> elem = it2.next();
            if (antecessor.equals(elem.getColumnKey())) {
                toAdd2.put(elem.getRowKey(), replacement, elem.getValue());
                it2.remove();
            }
        }

        edgeBetween.putAll(toAdd2);

        if (antecessor.equals(initialState)) {
            initialState = replacement;
        }
    }

    private @NotNull Collection<S> generateSingleState(@NotNull S state) {
        if (states.add(state)) {
            Map<ValuationSet, S> successors = state.getSuccessors();
            Map<S, ValuationSet> reverseMap = edgeBetween.row(state);

            // Insert all successors and construct reverse map.
            for (Entry<ValuationSet, S> transition : successors.entrySet()) {
                ValuationSet edge = transition.getKey();
                S successor = transition.getValue();

                ValuationSet vs = reverseMap.remove(successor);

                if (vs == null) {
                    vs = edge.clone();
                    transitions.put(state, edge, successor);
                } else if (mergingEnabled) {
                    transitions.remove(state, vs);
                    vs.addAll(edge);
                    transitions.put(state, vs, successor);
                } else {
                    transitions.put(state, edge, successor);
                    vs.addAll(edge);
                }

                reverseMap.put(successor, vs);
            }
        }

        return transitions.row(state).values();
    }
}
