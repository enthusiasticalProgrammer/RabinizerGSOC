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

import java.io.PrintStream;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import com.google.common.collect.Table.Cell;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rabinizer.collections.valuationset.ValuationSet;
import rabinizer.collections.valuationset.ValuationSetFactory;

public abstract class Automaton<S extends IState<S>> {

    protected final ValuationSetFactory valuationSetFactory;
    protected final boolean mergingEnabled;

    protected final Set<S> states;
    protected final Table<S, ValuationSet, S> transitions;
    protected final Table<S, S, ValuationSet> edgeBetween;
    protected @Nullable S initialState;
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

    protected Automaton(@NotNull Automaton<S> a) {
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
        return Collections.unmodifiableMap(transitions.row(state));
    }

    public int size() {
        return states.size();
    }

    public void toDotty(PrintStream p) {
        p.println("digraph \"Automaton for " + initialState + "\" \n{");

        for (IState<S> s : states) {
            if (s.equals(initialState)) {
                p.println("node [shape=oval, label=\"" + s + "\"]\"" + s + "\";");
            } else {
                p.println("node [shape=rectangle, label=\"" + s + "\"]\"" + s + "\";");
            }
        }

        for (Cell<S, ValuationSet, S> cell : transitions.cellSet()) {
            p.println("\"" + cell.getRowKey() + "\" -> \"" + cell.getColumnKey() + "\" [label=\"" + cell.getValue()
                    + "\"];");
        }
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

    /**
     * if the automaton is not complete anymore (e.g. because of optimization),
     * this method makes it complete by adding a trap state. If you use it after
     * the generation of the Acceptance-condition, either update the
     * Acceptance-condition or make sure, every generalized RabinPair is not a
     * Tautology (like Fin(emptySet)&Inf(allTransitions))
     */
    void makeComplete() {
        boolean usedTrapState = false;
        states.add(trapState);

        if (initialState == null) {
            initialState = trapState;
            usedTrapState = true;
        }

        Map<S, Map<ValuationSet, S>> trans = transitions.rowMap();

        for (S s : states) {
            ValuationSet vs = valuationSetFactory.createEmptyValuationSet();
            Set<Entry<ValuationSet, S>> transOfS;
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
                if (s != trapState) {
                    usedTrapState = true;
                }
            }
        }

        if (usedTrapState) {
            transitions.put(trapState, valuationSetFactory.createUniverseValuationSet(), trapState);
            edgeBetween.put(trapState, trapState, valuationSetFactory.createUniverseValuationSet());
            states.add(trapState);
        } else {
            states.remove(trapState);
        }
    }

    public List<Set<S>> SCCs() {
        return SCCAnalyser.SCCs(this, initialState);
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
    public void removeStates(@NotNull Set<S> statess) {
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

            Iterator<Table.Cell<S, ValuationSet, S>> it = transitions.cellSet().iterator();
            while (it.hasNext()) {
                if (statess.contains(it.next().getValue())) {
                    it.remove();
                }
            }
        }
    }

    // TODO to abstract ProductAutomaton ?
    protected @NotNull Set<ValuationSet> generatePartitioning(@NotNull Set<Set<ValuationSet>> product) {
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

    protected abstract @NotNull S generateInitialState();

    /**
     * @param scc: an SCC for which the transitions inside need to be determined
     * @return all transitions where start is in the SCC
     */

    protected @NotNull Set<Table.Cell<S, ValuationSet, S>> getTransitionsInSCC(@NotNull Set<S> scc) {
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
     *
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

        if (antecessor.equals(trapState)) {
            trapState = replacement;
        }

        if (antecessor.equals(initialState)) {
            initialState = replacement;
        }
    }

}
