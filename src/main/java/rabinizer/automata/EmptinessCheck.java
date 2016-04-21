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

import rabinizer.collections.valuationset.ValuationSet;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Christopher Ziegler
 */
public class EmptinessCheck<S extends IState<S>> {

    private final Automaton<S> automaton;
    private final Collection<GeneralizedRabinPair<S>> accTGR;

    /**
     * This method checks if the automaton is empty and it minimizes the
     * automaton together with the accTGR-acceptance condition if it is possible
     *
     * @param automaton
     * @param accTGR
     * @return true if the automaton accepts no words
     */
    public static <S extends IState<S>> boolean checkEmptinessAndMinimiseSCCBased(Automaton<S> automaton, Collection<GeneralizedRabinPair<S>> accTGR) {
        new EmptinessCheck(automaton, accTGR).minimiseSCCBased();
        return automaton.getStates().isEmpty();
    }

    private EmptinessCheck(Automaton<S> automaton, Collection<GeneralizedRabinPair<S>> accTGR) {
        this.automaton = automaton;
        this.accTGR = accTGR;
    }

    private void minimiseSCCBased() {
        for (Set<S> SCC : SCCAnalyser.SCCsStates(automaton)) {
            TranSet<S> tranSCC = SCCAnalyser.sccToTran(automaton, SCC);
            removeInterSCCAccConditions(SCC);

            boolean sccEmpty = true;

            for (GeneralizedRabinPair<S> pair : accTGR) {
                if (infAccepting(tranSCC, pair) && finAndInfAccepting(tranSCC, pair)) {
                    sccEmpty = false;
                } else {
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
        }
    }

    private void removeInterSCCAccConditions(Set<S> SCC) {
        for (S state : SCC) {
            Map<S, ValuationSet> relevantTransitions = automaton.transitions.get(state);
            for (Map.Entry<S, ValuationSet> transition : relevantTransitions.entrySet()) {
                if (!SCC.contains(transition.getKey())) {
                    for (GeneralizedRabinPair<S> pair : accTGR) {
                        pair.infs.forEach(inf -> inf.removeAll(state, transition.getValue()));
                        pair.fin.removeAll(state, transition.getValue());
                    }
                }
            }
        }
    }

    /**
     * @param scc: SCC for which the function is applied
     * @return true if for all inf-sets of the Rabin Pair, the SCC has a
     * transition in the inf-set
     */
    private static <S extends IState<S>> boolean infAccepting(TranSet<S> scc, GeneralizedRabinPair<S> pair) {
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
    private boolean finAndInfAccepting(TranSet<S> tranSCC, GeneralizedRabinPair<S> pair) {
        if (tranSCC.isEmpty()) {
            return false;
        }

        if (!tranSCC.intersects(pair.fin)) {
            return true;
        }

        // Compute SubSCCs without Fin-edges.
        List<TranSet<S>> subSCCs = SCCAnalyser.subSCCsTran(automaton, tranSCC, pair.fin);
        return subSCCs.stream().anyMatch(subSCC -> !subSCC.isEmpty() && infAccepting(subSCC, pair));
    }
}
