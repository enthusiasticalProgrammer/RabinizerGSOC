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

import org.jetbrains.annotations.NotNull;
import rabinizer.collections.valuationset.ValuationSet;
import rabinizer.collections.valuationset.ValuationSetFactory;

import java.util.*;
import java.util.function.Consumer;

public class TranSet<S> implements Iterable<Map.Entry<S, ValuationSet>> {

    private final Map<S, ValuationSet> backingMap;
    private final ValuationSetFactory factory;
    private final ValuationSet empty;

    public TranSet(ValuationSetFactory f) {
        factory = f;
        empty = f.createEmptyValuationSet();
        backingMap = new HashMap<>();
    }

    public Map<S, ValuationSet> asMap() {
        return Collections.unmodifiableMap(backingMap);
    }

    public void addAll(@NotNull S state, ValuationSet vs) {
        if (vs == null || vs.isEmpty()) {
            return;
        }

        ValuationSet valuationSet = backingMap.get(state);

        if (valuationSet == null) {
            valuationSet = vs.clone();
            backingMap.put(state, valuationSet);
        } else {
            valuationSet.addAll(vs);
        }
    }

    public void addAll(TranSet<S> other) {
        other.backingMap.forEach(this::addAll);
    }

    public boolean contains(@NotNull S state) {
        return backingMap.containsKey(state);
    }

    public boolean contains(@NotNull S state, Set<String> valuation) {
        return backingMap.getOrDefault(state, empty).contains(valuation);
    }

    public boolean containsAll(@NotNull S state, @NotNull ValuationSet vs) {
        return backingMap.getOrDefault(state, empty).containsAll(vs);
    }

    public boolean containsAll(@NotNull TranSet<S> other) {
        return other.backingMap.entrySet().stream()
                .allMatch(e -> containsAll(e.getKey(), e.getValue()));
    }

    public boolean intersects(TranSet<S> other) {
        return backingMap.entrySet().stream().anyMatch(e -> other.backingMap.getOrDefault(e.getKey(), empty).intersects(e.getValue()));
    }

    public boolean isEmpty() {
        return backingMap.isEmpty();
    }

    public void removeAll(Collection<S> states) {
        backingMap.keySet().removeAll(states);
    }

    public void removeAll(S state) {
        backingMap.remove(state);
    }

    public void removeAll(S state, ValuationSet vs) {
        ValuationSet valuationSet = backingMap.get(state);

        if (valuationSet == null || vs == null) {
            return;
        }

        valuationSet.removeAll(vs);

        if (valuationSet.isEmpty()) {
            backingMap.remove(state);
        }
    }

    public void removeAll(@NotNull TranSet<S> other) {
        other.backingMap.forEach(this::removeAll);
    }

    @Override
    public TranSet<S> clone() {
        TranSet<S> result = new TranSet<>(factory);
        result.addAll(this);
        return result;
    }

    @Override
    public Iterator<Map.Entry<S, ValuationSet>> iterator() {
        return backingMap.entrySet().iterator();
    }

    @Override
    public void forEach(Consumer<? super Map.Entry<S, ValuationSet>> action) {
        backingMap.entrySet().forEach(action);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TranSet<?> tranSet = (TranSet<?>) o;
        return Objects.equals(backingMap, tranSet.backingMap) &&
                Objects.equals(factory, tranSet.factory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(backingMap, factory);
    }

    @Override
    public String toString() {
        return backingMap.toString();
    }
}
