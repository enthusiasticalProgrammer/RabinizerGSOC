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

package rabinizer.collections;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class Collections3 {

    private Collections3() {

    }

    /**
     * Determine if the set is a singleton, meaning it exactly contains one element.
     *
     * @param set The set to be checked.
     * @return false if the the set is null or has not exactly one element.
     */
    @Contract("null -> false")
    public static <E> boolean isSingleton(@Nullable Set<E> set) {
        return set != null && set.size() == 1;
    }

    /**
     * Retrieve an arbitrary element from an {@code Iterable}.
     *
     * @param iterable
     * @param <E>
     * @return
     * @throws NoSuchElementException The methods throws an {@code NoSuchElementException} if iterable is either null
     *      or cannot provide an element.
     */
    @Contract("null -> fail")
    public static <E> E getElement(@Nullable Iterable<E> iterable) {
        if (iterable == null) {
            throw new NoSuchElementException();
        }

        return iterable.iterator().next();
    }

    /**
     * Remove an arbitrary element from an {@code Iterable}.
     *
     * @param iterable
     * @param <E>
     * @return
     * @throws NoSuchElementException The methods throws an {@code NoSuchElementException} if iterable is either null
     *      or cannot provide an element.
     */
    @Contract("null -> fail")
    public static <E> E removeElement(@Nullable Iterable<E> iterable) {
        if (iterable == null) {
            throw new NoSuchElementException();
        }

        Iterator<E> iter = iterable.iterator();
        E element = iter.next();
        iter.remove();
        return element;
    }

    public static @NotNull List<Integer> toList(@NotNull BitSet bs) {
        List<Integer> list = new ArrayList<>(bs.length());
        bs.stream().forEach(i -> list.add(i));
        return list;
    }
}
