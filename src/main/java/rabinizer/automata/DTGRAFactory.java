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

import rabinizer.automata.Product.ProductState;
import omega_automaton.Automaton;
import omega_automaton.acceptance.GeneralisedRabinAcceptance;
import omega_automaton.collections.TranSet;
import omega_automaton.collections.Tuple;
import omega_automaton.collections.valuationset.ValuationSetFactory;
import rabinizer.exec.Main;
import ltl.Formula;
import ltl.GOperator;
import ltl.equivalence.EquivalenceClassFactory;

import java.util.*;

import jhoafparser.consumer.HOAConsumerPrint;

public class DTGRAFactory {

    private DTGRAFactory() {

    }

    public static DTGRA constructDTGRA(Formula phi, EquivalenceClassFactory equivalenceClassFactory, ValuationSetFactory valuationSetFactory, Collection<Optimisation> opts) {
        Main.nonsilent("========================================");
        Main.nonsilent("Generating primaryAutomaton:\n");
        Master master;

        if (opts.contains(Optimisation.SLAVE_SUSPENSION)) {
            master = new SuspendedMaster(phi, equivalenceClassFactory, valuationSetFactory, opts);
        } else {
            master = new Master(phi, equivalenceClassFactory, valuationSetFactory, opts);
        }
        master.generate();
        if (!Main.silent) {
            HOAConsumerPrint hoa = new HOAConsumerPrint(System.out);
            master.toHOA(hoa, null);
        }

        Main.nonsilent("========================================");
        Main.nonsilent("Generating Mojmir & Rabin secondaryAutomata:\n");

        Set<GOperator> gSubformulas = phi.gSubformulas();
        Map<GOperator, RabinSlave> slaves = new HashMap<>();

        for (GOperator f : gSubformulas) {
            MojmirSlave mSlave;

            mSlave = new MojmirSlave(f, equivalenceClassFactory, valuationSetFactory, opts);
            mSlave.generate();

            RabinSlave rSlave = new RabinSlave(mSlave, valuationSetFactory);
            rSlave.generate();

            // remove transient part
            if (opts.contains(Optimisation.OPTIMISE_INITIAL_STATE)) {
                rSlave.optimizeInitialState();
            }

            slaves.put(f, rSlave);

            if (Main.verbose) {
                HOAConsumerPrint hoa = new HOAConsumerPrint(System.out);
                Main.verboseln("Mojmir Slave: ");
                mSlave.toHOA(hoa, null);

                Main.verboseln("\nRabin Slave: ");
                rSlave.toHOA(hoa, null);
            }
        }

        Main.nonsilent("========================================");
        Main.nonsilent("Generating product");

        Product automaton = new Product(master, slaves, valuationSetFactory, opts);
        automaton.generate();

        AccTGRRaw accTGR = null;

        if (opts.contains(Optimisation.COMPUTE_ACC_CONDITION)) {
            Main.nonsilent("========================================");
            Main.nonsilent("Generating local acceptance conditions");

            AccLocal accLocal = new AccLocal(automaton, valuationSetFactory, equivalenceClassFactory, opts);

            Main.nonsilent("========================================");
            Main.nonsilent("Generating global acceptance condition\n");
            accTGR = AccTGRRaw.createAccTGRRaw(accLocal, valuationSetFactory, automaton);

            Main.nonsilent("Generating optimized acceptance condition");
            AccTGRRaw.removeRedundancy(accTGR);
            Main.nonsilent("========================================");

            /**
             * Side effect: empty sink-SCCs get deleted, acceptance condition gets
             * reduced when possible
             *
             * @return true if automaton together witch acceptance condition is empty
             */
            if (opts.contains(Optimisation.EMPTINESS_CHECK)) {
                EmptinessCheck.checkEmptinessAndMinimiseSCCBased(automaton);
                AccTGRRaw.removeRedundancyLightAfterEmptyCheck(accTGR);
            }
        }

        return new DTGRA(automaton, accTGR);
    }

    public static class AccTGRRaw extends GeneralisedRabinAcceptance<ProductState> {

        private final Automaton<ProductState, ?> product;

        private AccTGRRaw(Automaton<ProductState, ?> product) {
            super(new ArrayList<Tuple<TranSet<ProductState>, List<TranSet<ProductState>>>>());
            this.product = product;
        }


        public static AccTGRRaw createAccTGRRaw(AccLocal accLocal, ValuationSetFactory factory, Product product) {
            AccTGRRaw accTGRRaw = new AccTGRRaw(product);

            Map<GOperator, Map<Set<GOperator>, Map<Integer, Tuple<TranSet<Product.ProductState>, TranSet<Product.ProductState>>>>> completeSlaveAcceptance = accLocal
                    .getAllSlaveAcceptanceConditions();
            for (Map.Entry<Map<GOperator, Integer>, TranSet<ProductState>> entry : accLocal.computeAccMasterOptions().entrySet()) {
                Map<GOperator, Integer> ranking = entry.getKey();
                Set<GOperator> gSet = ranking.keySet();

                TranSet<ProductState> Fin = new TranSet<>(factory);
                List<TranSet<ProductState>> Infs = new ArrayList<>();
                Fin.addAll(entry.getValue());

                for (GOperator g : gSet) {
                    Set<GOperator> localGSet = new HashSet<>(gSet);
                    localGSet.retainAll(accLocal.topmostGs.get(g));
                    Tuple<TranSet<Product.ProductState>, TranSet<Product.ProductState>> gPair;
                    gPair = completeSlaveAcceptance.get(g).get(localGSet).get(ranking.get(g));

                    Fin.addAll(gPair.left);
                    Infs.add(gPair.right.clone());
                }

                accTGRRaw.acceptanceCondition.add(new Tuple<>(Fin, Infs));
            }

            return accTGRRaw;
        }

        public static void removeRedundancy(AccTGRRaw this2) {
            List<TranSet<ProductState>> copy;
            int phase = 0;
            Main.stopwatchLocal();

            Main.verboseln(phase + ". Raw Generalized Rabin Acceptance Condition\n");
            printProgress(phase++, this2.acceptanceCondition.size());

            // This rule is subsumed by the following two rules.
            Main.verboseln(phase + ". Removing (F, {I1,...,In}) with complete F\n");
            this2.acceptanceCondition.removeIf(pair -> this2.product.containsAllTransitions(pair.left));
            printProgress(phase++, this2.acceptanceCondition.size());

            Main.verboseln(phase + ". Removing F from each Ii: (F, {I1,...,In}) |-> (F, {I1\\F,...,In\\F})\n");
            this2.acceptanceCondition.forEach(pair -> pair.right.forEach(inf -> inf.removeAll(pair.left)));
            printProgress(phase++, this2.acceptanceCondition.size());

            Main.verboseln(phase + ". Removing (F, {..., \\emptyset, ...} )\n");
            this2.acceptanceCondition.removeIf(pair -> pair.right.stream().anyMatch(TranSet::isEmpty));
            printProgress(phase++, this2.acceptanceCondition.size());

            Main.verboseln(phase + ". Removing complete Ii in (F, {I1,...,In}), i.e. Ii U F = Q \n");
            this2.acceptanceCondition.forEach(pair -> pair.right.removeIf(i ->
            this2.product.containsAllTransitions(i.union(pair.left))));
            printProgress(phase++, this2.acceptanceCondition.size());

            Main.verboseln(phase + ". Removing redundant Ii: (F, I) |-> (F, { i | i in I and !\\exists j in I : Ij <= Ii })\n");
            Collection<Tuple<TranSet<ProductState>, List<TranSet<ProductState>>>> temp = new ArrayList<>();
            for (Tuple<TranSet<ProductState>, List<TranSet<ProductState>>> pair : this2.acceptanceCondition) {
                copy = new ArrayList<>(pair.right);
                for (TranSet<ProductState> i : pair.right) {
                    for (TranSet<ProductState> j : pair.right) {
                        if (!j.equals(i) && i.containsAll(j)) {
                            copy.remove(i);
                            break;
                        }
                    }
                }
                temp.add(new Tuple<>(pair.left, copy));
            }
            this2.acceptanceCondition.clear();
            this2.acceptanceCondition.addAll(temp);

            printProgress(phase++, this2.acceptanceCondition.size());

            Main.verboseln(phase + ". Removing (F, I) for which there is a less restrictive (G, J) \n");


            Collection<Tuple<TranSet<ProductState>, List<TranSet<ProductState>>>> toRemove = new HashSet<>();
            for (Tuple<TranSet<ProductState>, List<TranSet<ProductState>>> pair1 : this2.acceptanceCondition) {
                for (Tuple<TranSet<ProductState>, List<TranSet<ProductState>>> pair2 : this2.acceptanceCondition) {
                    if (pair1.equals(pair2)) {
                        continue;
                    }

                    if (GeneralisedRabinAcceptance.implies(pair2, pair1) && !toRemove.contains(pair1)) {
                        toRemove.add(pair2);
                        break;
                    }
                }
            }

            this2.acceptanceCondition.clear();
            this2.acceptanceCondition.removeAll(toRemove);

            printProgress(phase, this2.acceptanceCondition.size());
        }

        public static void printProgress(int phase, int size) {
            Main.nonsilent("Phase: " + phase + "..." + size + " pairs");
        }

        public static void removeRedundancyLightAfterEmptyCheck(AccTGRRaw accTGR) {

            Collection<Tuple<TranSet<ProductState>, List<TranSet<ProductState>>>> toRemove = new HashSet<>();
            for (Tuple<TranSet<ProductState>, List<TranSet<ProductState>>> pair : accTGR.acceptanceCondition) {
                Set<TranSet<ProductState>> s = new HashSet<>(pair.right);
                pair.right.clear();
                pair.right.addAll(s);
                if (pair.right.stream().anyMatch(TranSet::isEmpty)) {
                    toRemove.add(pair);
                }
            }
            accTGR.acceptanceCondition.removeAll(toRemove);

            toRemove.clear();
            for (Tuple<TranSet<ProductState>, List<TranSet<ProductState>>> pair1 : accTGR.acceptanceCondition) {
                for (Tuple<TranSet<ProductState>, List<TranSet<ProductState>>> pair2 : accTGR.acceptanceCondition) {
                    if (pair1.equals(pair2)) {
                        continue;
                    }

                    if (GeneralisedRabinAcceptance.implies(pair2, pair1) && !toRemove.contains(pair1)) {
                        toRemove.add(pair2);
                        break;
                    }
                }
            }
            accTGR.acceptanceCondition.clear();
            accTGR.acceptanceCondition.removeAll(toRemove);
        }
    }
}
