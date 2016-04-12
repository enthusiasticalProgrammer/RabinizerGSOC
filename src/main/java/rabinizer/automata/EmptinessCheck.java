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

import rabinizer.collections.Collections3;
import rabinizer.collections.valuationset.ValuationSet;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

        for (Set<S> SCC : SCCAnalyser.SCCsStates(automaton)) {
            TranSet<S> tranSCC = SCCAnalyser.sccToTran(automaton, SCC);

            // remove inter-SCC-transitions
            for (Map.Entry<S, Map<S, ValuationSet>> entry : automaton.transitions.entrySet()) {
                S source = entry.getKey();

                for (Map.Entry<S, ValuationSet> transition : entry.getValue().entrySet()) {
                    ValuationSet label = transition.getValue();
                    S target = transition.getKey();

                    if (SCC.contains(source) && !SCC.contains(target)) {
                        for (GeneralizedRabinPair<S> pair : accTGR) {
                            pair.infs.forEach(inf -> inf.removeAll(source, label));
                            pair.fin.removeAll(source, label);
                        }
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

                    pair.infs.forEach(inf -> inf.removeAll(tranSCC));

                    if (!pair.infs.isEmpty()) {
                        pair.fin.removeAll(tranSCC);
                    }
                }
            }

            if (sccEmpty) {
                if (automaton.isSink(SCC)) {
                    automaton.removeStates(SCC);
                }
            }

            automatonEmpty = sccEmpty && automatonEmpty;
        }

        return automatonEmpty;
    }

    /**
     * @param scc: SCC for which the function is applied
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
     * @param scc:  current SCC
     * @param pair: current Rabin Pair
     * @return true if automaton accepts regarding this Rabin Pair & the current
     * SCC (i.e. if this Rabin Pair can accept a word, if the automaton
     * stays infinitely long in the current SCC)
     */
    private static <S extends IState<S>> boolean finAndInfAccepting(Automaton<S> automaton, TranSet<S> scc, GeneralizedRabinPair<S> pair) {
        if (scc.isEmpty()) {
            return false;
        }

        if (!scc.intersects(pair.fin)) {
            return true;
        }

        // Compute SubSCCs without Fin-edges.
        List<TranSet<S>> subSCCs = SCCAnalyser.subSCCs(automaton, scc, pair.fin);
        return subSCCs.stream().anyMatch(subSCC -> !subSCC.isEmpty() && infAccepting(subSCC, pair));
    }
}
