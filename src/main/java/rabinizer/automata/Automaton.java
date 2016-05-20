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

import jhoafparser.consumer.HOAConsumer;
import rabinizer.automata.output.HOAConsumerExtended;
import rabinizer.collections.valuationset.ValuationSet;
import rabinizer.collections.valuationset.ValuationSetFactory;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

public abstract class Automaton<S extends IState<S>> {

    @Nullable
    protected S initialState;
    protected final Map<S, Map<S, ValuationSet>> transitions;
    protected final ValuationSetFactory valuationSetFactory;

    protected Automaton(Automaton<S> a) {
        initialState = a.initialState;
        transitions = a.transitions;
        valuationSetFactory = a.valuationSetFactory;
    }

    protected Automaton(ValuationSetFactory factory) {
        transitions = new HashMap<>();
        valuationSetFactory = factory;
    }

    public void generate() {
        generate(getInitialState());
    }

    public void generate(S initialState) {
        // Return if already generated
        if (transitions.containsKey(initialState)) {
            return;
        }

        Queue<S> workList = new ArrayDeque<>();
        workList.add(initialState);

        while (!workList.isEmpty()) {
            S current = workList.remove();
            Collection<S> next = getSuccessors(current).keySet();

            for (S successor : next) {
                if (!transitions.containsKey(successor)) {
                    workList.add(successor);
                }
            }
        }
    }

    public boolean hasSuccessors(S state) {
        return !getSuccessors(state).isEmpty();
    }

    public boolean isSink(S state) {
        ValuationSet valuationSet = getSuccessors(state).get(state);
        return valuationSet != null && valuationSet.isUniverse();
    }

    public boolean isTransient(S state) {
        ValuationSet valuationSet = getSuccessors(state).get(state);
        return valuationSet == null || valuationSet.isEmpty();
    }

    @Nullable
    public S getSuccessor(S state, BitSet valuation) {
        for (Map.Entry<S, ValuationSet> transition : getSuccessors(state).entrySet()) {
            if (transition.getValue().contains(valuation)) {
                return transition.getKey();
            }
        }

        return null;
    }

    public Map<S, ValuationSet> getSuccessors(S state) {
        Map<S, ValuationSet> row = transitions.get(state);

        if (row == null) {
            row = state.getSuccessors();
            transitions.put(state, row);
        }

        return row;
    }

    public int size() {
        return transitions.size();
    }

    public S getInitialState() {
        if (initialState == null) {
            initialState = generateInitialState();
        }

        return initialState;
    }

    public Set<S> getStates() {
        return transitions.keySet();
    }

    public void removeUnreachableStates() {
        Set<S> states = new HashSet<>();
        states.add(getInitialState());
        removeUnreachableStates(states);
    }

    public void removeUnreachableStates(Set<S> reach) {
        getReachableStates(reach);
        removeStatesIf(s -> !reach.contains(s));
    }

    /**
     * This method removes unused states and their in- and outgoing transitions.
     * If the set dependsOn the initial state, it becomes an automaton with the
     * only state false. Use this method only if you are really sure you want to
     * remove the states! The method is designed for the assumptions, that only
     * nonaccepting SCCs are deleted, and the idea is also that everything,
     * which is deleted will be replaced with a trap state (in makeComplete).
     *
     * @param states: Set of states that is to be removed
     */
    public void removeStates(Collection<S> states) {
        if (states.contains(initialState)) {
            initialState = null;
            transitions.clear();
        } else {
            removeStatesIf(states::contains);
        }
    }

    public void removeStatesIf(Predicate<S> predicate) {
        transitions.keySet().removeIf(predicate);
        transitions.forEach((k, v) -> v.keySet().removeIf(predicate));

        if (predicate.test(initialState)) {
            initialState = null;
        }
    }

    public ValuationSetFactory getFactory() {
        return valuationSetFactory;
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
        return scc.stream().allMatch(s -> scc.containsAll(getSuccessors(s).keySet()));
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
        if (!(transitions.containsKey(antecessor) && transitions.containsKey(replacement))) {
            throw new IllegalArgumentException();
        }

        transitions.remove(antecessor).clear();

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

    private void getReachableStates(Set<S> states) {
        Deque<S> workList = new ArrayDeque<>(states);

        while (!workList.isEmpty()) {
            S state = workList.remove();

            getSuccessors(state).forEach((suc, v) -> {
                if (states.add(suc)) {
                    workList.add(suc);
                }
            });
        }
    }

    public final void toHOA(HOAConsumer ho) {
        if (getStates().isEmpty()) {
            HOAConsumerExtended.doHOAStatesEmpty(ho);
            return;
        }

        toHOABody(getConsumer(ho));
    }

    protected void toHOABody(HOAConsumerExtended<S, ?> hoa) {
        for (S s : getStates()) {
            hoa.addState(s);
            getSuccessors(s).forEach((k, v) -> hoa.addEdge(v, k));
            hoa.stateDone();
        }

        hoa.done();
    }

    protected HOAConsumerExtended<S, ?> getConsumer(HOAConsumer ho) {
        return new HOAConsumerExtended<>(ho, valuationSetFactory, null);
    }

    public boolean inputContainsAllAutomatonTransitions(TranSet<S> trans) {
        return this.getStates().stream().allMatch(state -> trans.containsAll(state, valuationSetFactory.createUniverseValuationSet()));
    }
}