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

import com.google.common.collect.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rabinizer.collections.valuationset.ValuationSet;
import rabinizer.collections.valuationset.ValuationSetFactory;

import java.util.*;
import java.util.function.Function;

public abstract class AbstractProductState<P extends IState<P>, K, S extends IState<S>, T> {

    protected final P primaryState;
    protected final Map<K, S> secondaryStates;

    protected AbstractProductState(P primaryState, Map<K, S> secondaryStates) {
        this.primaryState = primaryState;
        this.secondaryStates = secondaryStates;
    }

    protected AbstractProductState(P primaryState, Iterable<K> keys, Function<K, S> constructor) {
        this.primaryState = primaryState;

        ImmutableMap.Builder<K, S> builder = ImmutableMap.builder();
        keys.forEach(k -> builder.put(k, constructor.apply(k)));
        this.secondaryStates = builder.build();
    }

    // TODO: Drop getters?
    public P getPrimaryState() {
        return primaryState;
    }

    public S getSecondaryState(K key) {
        return secondaryStates.get(key);
    }

    public Map<K, S> getSecondaryMap() {
        return secondaryStates;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractProductState<?, ?, ?, ?> that = (AbstractProductState<?, ?, ?, ?>) o;
        return Objects.equals(primaryState, that.primaryState) &&
                Objects.equals(secondaryStates, that.secondaryStates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(primaryState, secondaryStates);
    }

    @Override
    public String toString() {
        return "(" + primaryState + "::" + secondaryStates + ')';
    }

    public @Nullable T getSuccessor(Set<String> valuation) {
        P primarySuccessor = primaryState.getSuccessor(valuation);

        if (primarySuccessor == null) {
            return null;
        }

        ImmutableMap.Builder<K, S> builder = ImmutableMap.builder();

        Set<K> keys = relevantSecondary(primarySuccessor);

        if (keys == null) {
            keys = secondaryStates.keySet();
        }

        for (K key : keys) {
            S secondary = secondaryStates.get(key);

            if (secondary != null) {
                S secondarySuccessor = secondary.getSuccessor(valuation);

                if (secondarySuccessor != null) {
                    builder.put(key, secondarySuccessor);
                } else {
                    return null;
                }
            }
        }

        return constructState(primarySuccessor, builder.build());
    }

    protected abstract Automaton<P> getPrimaryAutomaton();
    protected abstract Map<K, ? extends Automaton<S>> getSecondaryAutomata();

    public Map<ValuationSet, T> getSuccessors() {
        ImmutableMap.Builder<ValuationSet, T> builder = ImmutableMap.builder();

        Map<ValuationSet, P> primarySuccessors = getPrimaryAutomaton().getSuccessors(primaryState);
        Map<ValuationSet, Map<K, S>> secondarySuccessors = secondaryJointMove();

        for (Map.Entry<ValuationSet, P> entry1 : primarySuccessors.entrySet()) {
            for (Map.Entry<ValuationSet, Map<K, S>> entry2 : secondarySuccessors.entrySet()) {
                ValuationSet set = entry1.getKey().clone();
                set.retainAll(entry2.getKey());

                if (!set.isEmpty()) {
                    Set<K> keys = relevantSecondary(entry1.getValue());
                    Map<K, S> secondaryStates = entry2.getValue();

                    if (keys != null) {
                        Map<K, S> secondaryStates2 = new HashMap<>(secondaryStates.size());

                        for (K key : keys) {
                            secondaryStates2.put(key, secondaryStates.get(key));
                        }

                        secondaryStates = secondaryStates2;
                    }

                    builder.put(set, constructState(entry1.getValue(), secondaryStates));
                }
            }
        }

        return builder.build();
    }

    protected Map<ValuationSet, Map<K, S>> secondaryJointMove() {
        Map<K, ? extends Automaton<S>> secondary = getSecondaryAutomata();
        Map<ValuationSet, Map<K, S>> current = new HashMap<>();
        current.put(getFactory().createUniverseValuationSet(), Collections.emptyMap());

        for (Map.Entry<K, S> entry : secondaryStates.entrySet()) {
            K key = entry.getKey();
            S state = entry.getValue();

            Map<ValuationSet, S> successors = secondary.get(key).getSuccessors(state);
            Map<ValuationSet, Map<K, S>> next = new HashMap<>();

            for (Map.Entry<ValuationSet, Map<K, S>> entry1 : current.entrySet()) {
                for (Map.Entry<ValuationSet, S> entry2 : successors.entrySet()) {
                    ValuationSet set = entry1.getKey().clone();
                    set.retainAll(entry2.getKey());

                    if (!set.isEmpty()) {
                        Map<K, S> states = new HashMap<>(entry1.getValue());
                        states.put(key, entry2.getValue());

                        next.put(set, states);
                    }
                }
            }

            current = next;
        }

        return current;
    }

    public Set<String> getSensitiveAlphabet() {
        Set<String> sensitiveLetters = new HashSet<>(primaryState.getSensitiveAlphabet());

        for (S secondaryState : secondaryStates.values()) {
            sensitiveLetters.addAll(secondaryState.getSensitiveAlphabet());
        }

        return sensitiveLetters;
    }

    protected Set<K> relevantSecondary(P primaryState) {
        return null;
    }

    protected abstract T constructState(P primaryState, Map<K, S> secondaryStates);

    protected abstract @NotNull ValuationSetFactory getFactory();
}
