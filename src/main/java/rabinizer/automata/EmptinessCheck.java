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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import com.google.common.collect.Table.Cell;

import org.jetbrains.annotations.NotNull;
import rabinizer.collections.Tuple;
import rabinizer.collections.valuationset.ValuationSet;
import rabinizer.collections.valuationset.ValuationSetFactory;

/**
 * @author Christopher Ziegler
 */
public class EmptinessCheck<S extends IState<S>> {
    private final Automaton<S> automaton;
    private final List<TranSet<S>> sccs;
    private final Collection<? extends Tuple<TranSet<S>, Collection<TranSet<S>>>> accTGR;

    private EmptinessCheck(Automaton<S> automaton,
                           Collection<? extends Tuple<TranSet<S>, Collection<TranSet<S>>>> accTGR) {
        this.automaton = automaton;
        this.sccs = this.automaton.SCCs();
        this.accTGR = accTGR;
    }

    /**
     * This method checks if the automaton is empty and it minimizes the
     * automaton together with the accTGR-acceptance condition if it is possible
     *
     * @param automaton
     * @param accTGR
     * @return true if the automaton accepts no words
     */
    public static <S extends IState<S>> boolean checkEmptiness(Automaton<S> automaton,
                                                               Collection<? extends Tuple<TranSet<S>, Collection<TranSet<S>>>> accTGR) {
        return new EmptinessCheck<>(automaton, accTGR).checkIfEmpty();
    }

    private boolean checkIfEmpty() {
        boolean automatonEmpty = true;

        for (TranSet<S> tranSCC : sccs) {
            Set<S> scc = tranSCC.asMap().keySet();
            boolean sccEmpty = true;

            // remove inter-SCC-transitions
            for (Cell<S, ValuationSet, S> entry : automaton.transitions.cellSet()) {
                if (scc.contains(entry.getRowKey()) && !scc.contains(entry.getValue())) {
                    for (Tuple<TranSet<S>, Collection<TranSet<S>>> pair : accTGR) {
                        pair.right.forEach(inf -> inf.removeAll(entry.getRowKey(), entry.getColumnKey()));
                        pair.left.removeAll(entry.getRowKey(), entry.getColumnKey());
                    }
                }
            }

            for (Tuple<TranSet<S>, Collection<TranSet<S>>> pair : accTGR) {
                if (allInfSetsOfRabinPairPresentInSCC(tranSCC, pair) && checkForFinTransitions(scc, pair)) {
                    sccEmpty = false;
                } else {
                    // all components of infinite
                    // sets in current scc can be
                    // deleted and all components of finite sets of current scc
                    // if any infinite condition is present

                    // @Christopher TODO: Check this.
                    pair.right.forEach(inf -> inf.removeAll(scc));

                    if (!pair.right.isEmpty()) {
                        pair.left.removeAll(scc);
                    }
                }
            }

            if (sccEmpty) {
                if (automaton.isSink(scc)) {
                    automaton.removeStates(scc);
                }
            }

            automatonEmpty = sccEmpty && automatonEmpty;
        }

        return automatonEmpty;
    }

    /**
     * @param scc:   SCC for which the function is applied
     * @return true if for all inf-sets of the Rabin Pair, the SCC has a
     * transition in the inf-set
     */
    private static <S> boolean allInfSetsOfRabinPairPresentInSCC(TranSet<S> scc, Tuple<TranSet<S>, Collection<TranSet<S>>> pair) {
        return pair.right.stream().allMatch(inf -> inf.intersect(scc));
    }

    private static <E> boolean isSingleton(@NotNull Set<E> set) {
        return set.size() == 1;
    }

    private static <E> E getElement(@NotNull Iterable<E> collection) {
        return collection.iterator().next();
    }

    /**
     * Precondition: allInfSetsOfRabinPairPresentInSCC with the same arguments
     * has to be true
     *
     * @param scc:   current SCC
     * @param pair:  current Rabin Pair
     * @return true if automaton accepts regarding this Rabin Pair & the current
     * SCC (i.e. if this Rabin Pair can accept a word, if the automaton
     * stays infinitely long in the current SCC)
     */
    private boolean checkForFinTransitions(Set<S> scc, Tuple<TranSet<S>, Collection<TranSet<S>>> pair) {
        if (isSingleton(scc) && !automaton.isLooping(getElement(scc))) {
            return false;
        }

        if (Sets.intersection(scc, pair.left.asMap().entrySet()).isEmpty()) {
            return true;
        }

        // Compute SubSCCs without Fin-edges.
        List<TranSet<S>> subSCCs = automaton.subSCCs(scc, pair.left);
        return subSCCs.stream().anyMatch(subSCC -> allInfSetsOfRabinPairPresentInSCC(subSCC, pair));
    }
}
