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
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import com.google.common.collect.Table.Cell;

import rabinizer.collections.Collections3;
import rabinizer.collections.Tuple;
import rabinizer.collections.valuationset.ValuationSet;

/**
 * @author Christopher Ziegler
 */
public class EmptinessCheck {

    /**
     * This method checks if the automaton is empty and it minimizes the
     * automaton together with the accTGR-acceptance condition if it is possible
     *
     * @param automaton
     * @param accTGR
     * @return true if the automaton accepts no words
     */
    public static <S extends IState<S>> boolean checkEmptiness(Automaton<S> automaton, Collection<GeneralizedRabinPair<S>> accTGR) {
        return checkIfEmpty(automaton, accTGR);
    }

    private static <S extends IState<S>> boolean checkIfEmpty(Automaton<S> automaton, Collection<GeneralizedRabinPair<S>> accTGR) {
        boolean automatonEmpty = true;

        for (TranSet<S> tranSCC : automaton.SCCs()) {
            // remove inter-SCC-transitions
            for (Cell<S, ValuationSet, S> entry : automaton.transitions.cellSet()) {
                S soure = entry.getRowKey();
                ValuationSet label = entry.getColumnKey();
                S target = entry.getValue();

                if (tranSCC.contains(soure) && !tranSCC.contains(target)) {
                    for (GeneralizedRabinPair<S> pair : accTGR) {
                        pair.infs.forEach(inf -> inf.removeAll(soure, label));
                        pair.fin.removeAll(soure, label);
                    }
                }
            }

            boolean sccEmpty = true;

            for (GeneralizedRabinPair<S> pair : accTGR) {
                if (infAccepting(tranSCC, pair) && finAndInfAccepting(automaton, tranSCC, pair)) {
                    sccEmpty = false;
                } else {
                    // all components of infinite
                    // sets in current scc can be
                    // deleted and all components of finite sets of current scc
                    // if any infinite condition is present

                    // @Christopher TODO: Check this.
                    pair.infs.forEach(inf -> inf.removeAll(tranSCC));

                    if (!pair.infs.isEmpty()) {
                        pair.fin.removeAll(tranSCC);
                    }
                }
            }

            if (sccEmpty) {
                Set<S> scc = tranSCC.asMap().keySet();

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
    private static <S> boolean infAccepting(TranSet<S> scc, GeneralizedRabinPair<S> pair) {
        return pair.infs.stream().allMatch(inf -> inf.intersects(scc));
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
    private static <S extends IState<S>> boolean finAndInfAccepting(Automaton<S> automaton, TranSet<S> scc, GeneralizedRabinPair<S> pair) {
        if (Collections3.isSingleton(scc.asMap().keySet()) && !automaton.isLooping(Collections3.getElement(scc.asMap().keySet()))) {
            return false;
        }

        if (!scc.intersects(pair.fin)) {
            return true;
        }

        // Compute SubSCCs without Fin-edges.
        List<TranSet<S>> subSCCs = automaton.subSCCs(scc, pair.fin);
        return subSCCs.stream().anyMatch(subSCC -> infAccepting(subSCC, pair));
    }
}
