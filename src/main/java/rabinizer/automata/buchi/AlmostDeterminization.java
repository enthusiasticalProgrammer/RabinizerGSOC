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

package rabinizer.automata.buchi;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rabinizer.collections.Tuple;

import java.util.*;

public class AlmostDeterminization {

    private final BuchiAutomaton automaton;
    private final BuchiAutomaton semi;
    private final BiMap<Set<BuchiAutomaton.State>, BuchiAutomaton.State> cMapping;
    private final BiMap<Tuple<Set<BuchiAutomaton.State>, Set<BuchiAutomaton.State>>, BuchiAutomaton.State> dMapping;

    public AlmostDeterminization(BuchiAutomaton automaton) {
        this.automaton = automaton;
        this.semi = new BuchiAutomaton(automaton.valuationSetFactory);
        this.semi.acceptingStates.clear();
        this.dMapping = HashBiMap.create();
        this.cMapping = HashBiMap.create();
    }

    public BuchiAutomaton apply() {
        if (automaton.isDeterministic()) {
            return automaton;
        }

        Set<BuchiAutomaton.State> visited = new HashSet<>();
        Deque<BuchiAutomaton.State> workList = new ArrayDeque<>();

        cMapping.put(Collections.singleton(automaton.initialState), semi.initialState);
        workList.add(semi.initialState);
        semi.initialState.label = Collections.singleton(automaton.initialState).toString();

        while (!workList.isEmpty()) {
            BuchiAutomaton.State current = workList.remove();

            if (visited.contains(current)) {
                continue;
            }

            // Check if current is in the C component
            Set<BuchiAutomaton.State> states = cMapping.inverse().get(current);

            if (states != null) {
                for (Set<String> valuation : Sets.powerSet(automaton.valuationSetFactory.getAlphabet())) {
                    BuchiAutomaton.State successor = getCSuccessor(states, valuation);

                    if (successor != null) {
                        semi.addTransition(current, valuation, successor);
                        workList.add(successor);
                    }
                }
            }

            for (BuchiAutomaton.State state : Sets.intersection(states != null ? states : Collections.emptySet(), automaton.acceptingStates)) {
                for (Set<String> valuation : Sets.powerSet(automaton.valuationSetFactory.getAlphabet())) {
                    BuchiAutomaton.State successor = getDSuccessor(new Tuple<>(Collections.singleton(state), Collections.singleton(state)), valuation);

                    if (successor != null) {
                        semi.addTransition(current, valuation, successor);
                        workList.add(successor);
                    }
                }
            }

            // Check if current is in the D component
            Tuple<Set<BuchiAutomaton.State>, Set<BuchiAutomaton.State>> tuple = dMapping.inverse().get(current);

            if (tuple != null) {
                for (Set<String> valuation : automaton.valuationSetFactory.createUniverseValuationSet()) {
                    BuchiAutomaton.State successor = getDSuccessor(tuple, valuation);

                    if (successor != null) {
                        semi.addTransition(current, valuation, successor);
                        workList.add(successor);
                    }
                }
            }

            visited.add(current);
        }

        // Compute Accepting States
        for (Map.Entry<Tuple<Set<BuchiAutomaton.State>, Set<BuchiAutomaton.State>>, BuchiAutomaton.State> entry : dMapping.entrySet()) {
            Tuple<Set<BuchiAutomaton.State>, Set<BuchiAutomaton.State>> tuple = entry.getKey();

            if (!Sets.intersection(tuple.left, automaton.acceptingStates).isEmpty() && tuple.left.equals(tuple.right)) {
                semi.setAccepting(entry.getValue());
            }
        }

        return semi;
    }

    private @Nullable BuchiAutomaton.State getCSuccessor(@NotNull Set<BuchiAutomaton.State> states, @NotNull Set<String> valuation) {
        Set<BuchiAutomaton.State> successors = new HashSet<>(states.size());

        for (BuchiAutomaton.State state : states) {
            successors.addAll(automaton.getTransitions(state, valuation));
        }

        // Don't construct the trap state.
        if (successors.isEmpty()) {
            return null;
        }

        BuchiAutomaton.State succ = cMapping.get(successors);

        if (succ == null) {
            succ = semi.createState(successors.toString());
            cMapping.put(successors, succ);
        }

        return succ;
    }

    private @Nullable BuchiAutomaton.State getDSuccessor(Tuple<Set<BuchiAutomaton.State>, Set<BuchiAutomaton.State>> tuple, final Set<String> valuation) {
        // Standard Subset Construction
        Set<BuchiAutomaton.State> rightSuccessor = new HashSet<>();

        for (BuchiAutomaton.State rightState : tuple.right) {
            rightSuccessor.addAll(automaton.getTransitions(rightState, valuation));
        }

        Set<BuchiAutomaton.State> leftSuccessor = new HashSet<>();

        // Add all states reached from an accepting state
        tuple.right.stream()
                .filter(automaton.acceptingStates::contains)
                .forEach(s -> leftSuccessor.addAll(automaton.getTransitions(s, valuation)));

        if (!tuple.left.equals(tuple.right)) {
            tuple.left.forEach(s -> leftSuccessor.addAll(automaton.getTransitions(s, valuation)));
        }

        // Don't construct the trap state.
        if (leftSuccessor.isEmpty() && rightSuccessor.isEmpty()) {
            return null;
        }

        Tuple<Set<BuchiAutomaton.State>, Set<BuchiAutomaton.State>> prod = new Tuple<>(ImmutableSet.copyOf(leftSuccessor), ImmutableSet.copyOf(rightSuccessor));

        BuchiAutomaton.State succ = dMapping.get(prod);

        if (succ == null) {
            succ = semi.createState(prod.toString());
            dMapping.put(prod, succ);
        }

        return succ;
    }
}
