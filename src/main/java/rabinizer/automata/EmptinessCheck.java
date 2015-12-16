package rabinizer.automata;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Table.Cell;

import rabinizer.exec.Tuple;
import rabinizer.ltl.ValuationSet;
import rabinizer.ltl.ValuationSetFactory;

/**
 * @author Christopher Ziegler
 */
public class EmptinessCheck<State> {
    private final Automaton<State> automaton;
    private final List<Set<State>> sccs;
    private final Collection<? extends Tuple<TranSet<State>, Set<TranSet<State>>>> accTGR;
    private final ValuationSetFactory valuationFactory;

    /**
     * This method checks if the automaton is empty and it minimizes the
     * automaton together with the accTGR-acceptance condition if it is possible
     * 
     * @param automaton
     * @param accTGR
     * @param valuationFactory
     * @return true if the automaton accepts no words
     */
    public static <State> boolean checkEmptiness(Automaton<State> automaton,
            Collection<? extends Tuple<TranSet<State>, Set<TranSet<State>>>> accTGR,
            ValuationSetFactory<String> valuationFactory) {
        return new EmptinessCheck<State>(automaton, accTGR, valuationFactory).checkIfEmpty();

    }

    private EmptinessCheck(Automaton<State> automaton,
            Collection<? extends Tuple<TranSet<State>, Set<TranSet<State>>>> accTGR,
            ValuationSetFactory valuationFactory) {
        this.automaton = automaton;
        this.sccs = this.automaton.SCCs();
        this.accTGR = accTGR;
        this.valuationFactory = valuationFactory;
    }

    private boolean checkIfEmpty() {
        boolean automatonEmpty = true;
        for (Set<State> scc : sccs) {
            boolean sccEmpty = true;
            Set<Cell<State, ValuationSet, State>> trans1 = automaton.getTransitionsInSCC(scc);
            Map<State, ValuationSet> trans = new HashMap<State, ValuationSet>();
            for (State s : scc) {
                trans.put(s, valuationFactory.createEmptyValuationSet());
            }
            for (Cell<State, ValuationSet, State> entry : trans1) {
                ValuationSet val = trans.get(entry.getRowKey());
                val.addAll(entry.getColumnKey());
                trans.put(entry.getRowKey(), val);
                if (!scc.contains(entry.getValue())) {
                    for (Tuple<TranSet<State>, Set<TranSet<State>>> pair : accTGR) {
                        for (TranSet<State> inf : pair.getRight()) {
                            if (inf.get(entry.getRowKey()) != null) {
                                ValuationSet valu = valuationFactory.createValuationSet(inf.get(entry.getRowKey()));
                                valu.retainAll(entry.getColumnKey());
                                inf.put(entry.getRowKey(), valu);
                            }
                        }
                        TranSet<State> fin = pair.getLeft();
                        if (fin.get(entry.getRowKey()) != null) {
                            ValuationSet valu = valuationFactory.createValuationSet(fin.get(entry.getRowKey()));
                            valu.retainAll(entry.getColumnKey());
                            fin.put(entry.getRowKey(), valu);
                        }
                    }
                }
            }

            for (Tuple<TranSet<State>, Set<TranSet<State>>> pair : accTGR) {

                if (!allInfSetsOfRabinPairPresentInSCC(scc, pair, trans) || checkForFinTransitions(scc, pair, trans)) {
                    // all components of infinite
                    // sets in current scc can be
                    // deleted and all components of finite sets of current scc
                    // if any infinite condition is present

                    for (TranSet<State> inf : pair.getRight()) {
                        for (State s : scc) {
                            inf.remove(s);
                        }
                    }
                    if (pair.getRight().stream().anyMatch(infCond -> true)) {
                        for (State s : scc) {
                            pair.getLeft().remove(s);
                        }
                    }
                } else {
                    sccEmpty = false;
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
     * 
     * @param scc:
     *            SCC for which the function is applied
     * @param pair:
     *            Rabin Pair
     * @param trans:
     *            Transition of the scc
     * @return true if for all inf-sets of the Rabin Pair, the SCC has a
     *         transition in the inf-set
     */
    private boolean allInfSetsOfRabinPairPresentInSCC(Set<State> scc, Tuple<TranSet<State>, Set<TranSet<State>>> pair,
            Map<State, ValuationSet> trans) {

        boolean allInfs = true;
        for (TranSet<State> inf : pair.getRight()) {
            Set<Map.Entry<State, ValuationSet>> intersect1 = inf.entrySet();
            Map<State, ValuationSet> intersect = new HashMap<State, ValuationSet>();
            for (Map.Entry<State, ValuationSet> i : intersect1) {
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

            allInfs = allInfs && intersect.entrySet().stream().anyMatch(entry -> entry.getValue() != null
                    && scc.contains(automaton.transitions.get(entry.getKey(), entry.getValue())));

        }
        return allInfs;
    }

    /**
     * Precondition: allInfSetsOfRabinPairPresentInSCC with the same arguments
     * has to be true
     * 
     * @param scc:
     *            current SCC
     * @param pair:
     *            current Rabin Pair
     * @param trans:
     *            transitions of current SCC
     * @return true if automaton accepts regarding this Rabin Pair & the current
     *         SCC (i.e. if this Rabin Pair can accept a word, if the automaton
     *         stays infinitely long in the current SCC)
     */
    private boolean checkForFinTransitions(Set<State> scc, Tuple<TranSet<State>, Set<TranSet<State>>> pair,
            Map<State, ValuationSet> trans) {
        Set<Map.Entry<State, ValuationSet>> intersect1 = pair.getLeft().entrySet();

        Map<State, ValuationSet> intersect = new HashMap<State, ValuationSet>();
        for (Map.Entry<State, ValuationSet> i : intersect1) {
            if (scc.contains(i.getKey())) {
                if (intersect.get(i.getKey()) != null) {
                    ValuationSet val = intersect.get(i.getKey()).complement().clone();
                    val.retainAll(trans.get(i.getKey()));
                    intersect.put(i.getKey(), val);
                } else {
                    intersect.put(i.getKey(), trans.get(i.getKey()));
                }
            }
        }
        if (intersect.isEmpty()) {
            return false;
        } else {
            List<Set<State>> subSCC = automaton.subSCCs(scc, intersect);
            return subSCC.stream()
                    .allMatch(sub -> pair.getRight().stream().allMatch(c -> !Collections.disjoint(c.keySet(), sub)));
        }
    }

}
