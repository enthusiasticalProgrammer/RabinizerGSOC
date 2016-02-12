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

import com.google.common.collect.Table.Cell;

import rabinizer.collections.Tuple;
import rabinizer.collections.valuationset.ValuationSet;
import rabinizer.collections.valuationset.ValuationSetFactory;

/**
 * @author Christopher Ziegler
 */
public class EmptinessCheck<S extends IState<S>> {
    private final Automaton<S> automaton;
    private final List<Set<S>> sccs;
    private final Collection<? extends Tuple<TranSet<S>, Set<TranSet<S>>>> accTGR;
    private final ValuationSetFactory valuationFactory;

    private EmptinessCheck(Automaton<S> automaton,
                           Collection<? extends Tuple<TranSet<S>, Set<TranSet<S>>>> accTGR) {
        this.automaton = automaton;
        this.sccs = this.automaton.SCCs();
        this.accTGR = accTGR;
        this.valuationFactory = automaton.valuationSetFactory;
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
                                                               Collection<? extends Tuple<TranSet<S>, Set<TranSet<S>>>> accTGR) {
        return new EmptinessCheck<>(automaton, accTGR).checkIfEmpty();

    }

    private boolean checkIfEmpty() {
        boolean automatonEmpty = true;
        for (Set<S> scc : sccs) {
            boolean sccEmpty = true;
            Set<Cell<S, ValuationSet, S>> trans1 = automaton.getTransitionsInSCC(scc);
            Map<S, ValuationSet> trans = new HashMap<>();// Transitions, that
            // are "really in the
            // SCC (begin+end)"
            for (S s : scc) {
                trans.put(s, valuationFactory.createEmptyValuationSet());
            }

            // remove inter-SCC-transitions from any set, and crowd trans
            for (Cell<S, ValuationSet, S> entry : trans1) {
                if (scc.contains(entry.getValue())) {
                    ValuationSet val = trans.get(entry.getRowKey());
                    val.addAll(entry.getColumnKey());
                    trans.put(entry.getRowKey(), val);
                } else {
                    for (Tuple<TranSet<S>, Set<TranSet<S>>> pair : accTGR) {
                        for (TranSet<S> inf : pair.right) {
                            if (inf.get(entry.getRowKey()) != null) {
                                ValuationSet valu = inf.get(entry.getRowKey()).clone();
                                valu.retainAll(entry.getColumnKey().complement());
                                inf.put(entry.getRowKey(), valu);
                            }
                        }
                        TranSet<S> fin = pair.left;
                        if (fin.get(entry.getRowKey()) != null) {
                            ValuationSet valu = fin.get(entry.getRowKey()).clone();
                            valu.retainAll(entry.getColumnKey().complement());
                            fin.put(entry.getRowKey(), valu);
                        }
                    }
                }
            }


            for (Tuple<TranSet<S>, Set<TranSet<S>>> pair : accTGR) {
                if (allInfSetsOfRabinPairPresentInSCC(scc, pair, trans) && checkForFinTransitions(scc, pair, trans)) {
                    sccEmpty = false;
                } else {
                    // all components of infinite
                    // sets in current scc can be
                    // deleted and all components of finite sets of current scc
                    // if any infinite condition is present

                    for (TranSet<S> inf : pair.right) {
                        for (S s : scc) {
                            inf.remove(s);
                        }
                    }
                    if (pair.right.stream().anyMatch(infCond -> true)) {
                        for (S s : scc) {
                            pair.left.remove(s);
                        }
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
     * @param pair:  Rabin Pair
     * @param trans: Transition of the scc
     * @return true if for all inf-sets of the Rabin Pair, the SCC has a
     * transition in the inf-set
     */
    private boolean allInfSetsOfRabinPairPresentInSCC(Set<S> scc, Tuple<TranSet<S>, Set<TranSet<S>>> pair,
                                                      Map<S, ValuationSet> trans) {

        boolean allInfs = true;
        for (TranSet<S> inf : pair.right) {
            Set<Map.Entry<S, ValuationSet>> intersect1 = new HashSet<>(inf.entrySet());
            Map<S, ValuationSet> intersect = new HashMap<>();
            for (Map.Entry<S, ValuationSet> i : intersect1) {
                if (scc.contains(i.getKey())) {
                    if (intersect.get(i.getKey()) != null) {
                        ValuationSet val = intersect.get(i.getKey()).clone();
                        val.retainAll(trans.get(i.getKey()));
                        intersect.put(i.getKey(), val);
                    } else {
                        intersect.put(i.getKey(), trans.get(i.getKey()));
                    }
                }
            }

            allInfs = allInfs && automaton.transitions.cellSet().stream().filter(
                    entry -> intersect.get(entry.getRowKey()) != null && intersect.get(entry.getRowKey()).containsAll((entry.getColumnKey())) && scc.contains(entry.getValue()))
                    .anyMatch(entry -> true);

        }
        return allInfs;
    }

    /**
     * Precondition: allInfSetsOfRabinPairPresentInSCC with the same arguments
     * has to be true
     *
     * @param scc:   current SCC
     * @param pair:  current Rabin Pair
     * @param trans: transitions of current SCC
     * @return true if automaton accepts regarding this Rabin Pair & the current
     * SCC (i.e. if this Rabin Pair can accept a word, if the automaton
     * stays infinitely long in the current SCC)
     */
    private boolean checkForFinTransitions(Set<S> scc, Tuple<TranSet<S>, Set<TranSet<S>>> pair,
                                           Map<S, ValuationSet> trans) {
        Set<Map.Entry<S, ValuationSet>> intersect1 = new HashSet<>(pair.left.entrySet());

        Map<S, ValuationSet> intersect = new HashMap<>();
        for (Map.Entry<S, ValuationSet> i : intersect1) {
            if (scc.contains(i.getKey())) {
                if (intersect.get(i.getKey()) != null) {
                    ValuationSet val = intersect.get(i.getKey()).complement().clone();
                    val.retainAll(trans.get(i.getKey()));
                    intersect.put(i.getKey(), val);
                } else {
                    ValuationSet valu = trans.get(i.getKey()).clone();
                    valu.retainAll(i.getValue());
                    if (!valu.isEmpty()) {
                        intersect.put(i.getKey(), valu);
                    }
                }
            }
        }
        if (intersect.isEmpty()) {
            return true;
        } else {
            List<Set<S>> subSCC = automaton.subSCCs(scc, intersect);
            return subSCC.stream().anyMatch(sub -> pair.right.stream().allMatch(c -> !Collections.disjoint(c.keySet(), sub)));
        }
    }

}
