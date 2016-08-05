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

import omega_automaton.Automaton;
import omega_automaton.AutomatonState;
import omega_automaton.Edge;
import omega_automaton.acceptance.GeneralisedRabinAcceptance;
import omega_automaton.algorithms.SCCAnalyser;
import omega_automaton.collections.TranSet;
import omega_automaton.collections.Tuple;
import omega_automaton.collections.valuationset.ValuationSet;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class EmptinessCheck<S extends AutomatonState<S>> {

    private final Automaton<S, ? extends GeneralisedRabinAcceptance<S>> automaton;

    /**
     * This method checks if the automaton is empty and it minimizes the
     * automaton together with the accTGR-acceptance condition if it is possible
     *
     * @param automaton
     * @return true if the automaton accepts no words
     */
    public static boolean checkEmptinessAndMinimiseSCCBasedProduct(Product p) {
        if (!(p.getAcceptance() instanceof GeneralisedRabinAcceptance)) {
            throw new IllegalArgumentException("We can (yet) only perform the Emptiness-check for GeneralisedRabinAcceptance.");
        }
        new EmptinessCheck<>(p).minimiseSCCBased();
        return p.getStates().isEmpty();
    }

    private EmptinessCheck(Automaton<S, GeneralisedRabinAcceptance<S>> automaton) {
        this.automaton = automaton;
    }

    private void minimiseSCCBased() {
        for (Set<S> SCC : SCCAnalyser.SCCsStates(automaton)) {
            TranSet<S> tranSCC = SCCAnalyser.sccToTran(automaton, SCC, new TranSet<S>(automaton.getFactory()));
            removeInterSCCAccConditions(SCC);

            boolean sccEmpty = true;

            for (Tuple<TranSet<S>, List<TranSet<S>>> pair : automaton.getAcceptance().acceptanceCondition) {
                if (infAccepting(tranSCC, pair) && finAndInfAccepting(tranSCC, pair)) {
                    sccEmpty = false;
                } else {
                    pair.right.forEach(inf -> inf.removeAll(tranSCC));
                    if (!pair.right.isEmpty()) {
                        pair.left.removeAll(tranSCC);
                    }
                }
            }

            if (sccEmpty) {
                if (automaton.isBSCC(SCC)) {
                    automaton.removeStates(SCC);
                }
            }
        }
    }

    private void removeInterSCCAccConditions(Set<S> SCC) {
        for (S state : SCC) {
            Map<Edge<S>, ValuationSet> relevantTransitions = automaton.getSuccessors(state);
            for (Map.Entry<Edge<S>, ValuationSet> transition : relevantTransitions.entrySet()) {
                if (!SCC.contains(transition.getKey().successor)) {
                    for (Tuple<TranSet<S>, List<TranSet<S>>> pair : automaton.getAcceptance().acceptanceCondition) {
                        pair.right.forEach(inf -> inf.removeAll(state, transition.getValue()));
                        pair.left.removeAll(state, transition.getValue());
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
    private static <S extends AutomatonState<S>> boolean infAccepting(TranSet<S> scc, Tuple<TranSet<S>, List<TranSet<S>>> pair) {
        return pair.right.stream().allMatch(inf -> inf.intersects(scc));
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
    private boolean finAndInfAccepting(TranSet<S> tranSCC, Tuple<TranSet<S>, List<TranSet<S>>> pair) {
        if (tranSCC.isEmpty()) {
            return false;
        }

        if (!tranSCC.intersects(pair.left)) {
            return true;
        }

        // Compute SubSCCs without Fin-edges.
        List<TranSet<S>> subSCCs = SCCAnalyser.subSCCsTran(automaton, tranSCC, pair.left);
        return subSCCs.stream().anyMatch(subSCC -> !subSCC.isEmpty() && infAccepting(subSCC, pair));
    }
}
