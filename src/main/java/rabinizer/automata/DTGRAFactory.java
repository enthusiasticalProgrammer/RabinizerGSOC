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
import rabinizer.collections.valuationset.ValuationSetFactory;
import rabinizer.exec.Main;
import rabinizer.ltl.Formula;
import rabinizer.ltl.GOperator;
import rabinizer.ltl.equivalence.EquivalenceClassFactory;

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
            master.toHOA(hoa);
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
                mSlave.toHOA(hoa);

                Main.verboseln("\nRabin Slave: ");
                rSlave.toHOA(hoa);
            }
        }

        Main.nonsilent("========================================");
        Main.nonsilent("Generating product");

        Product automaton = new Product(master, slaves, valuationSetFactory, opts);
        automaton.generate();

        AccTGRRaw<ProductState> accTGR = null;

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
                EmptinessCheck.checkEmptinessAndMinimiseSCCBased(automaton, accTGR);
                AccTGRRaw.removeRedundancyLightAfterEmptyCheck(accTGR);
            }
        }

        return new DTGRA(automaton, accTGR);
    }

    public static class AccTGRRaw<S extends IState<S>> extends LinkedList<GeneralizedRabinPair<S>> {

        private static final long serialVersionUID = 245172601429256815L;

        private final Automaton<S> product;

        private AccTGRRaw(Automaton<S> product) {
            this.product = product;
        }


        public static AccTGRRaw<ProductState> createAccTGRRaw(AccLocal accLocal, ValuationSetFactory factory, Product product) {
            AccTGRRaw<ProductState> accTGRRaw = new AccTGRRaw<>(product);

            Map<GOperator, Map<Set<GOperator>, Map<Integer, RabinPair<Product.ProductState>>>> completeSlaveAcceptance = accLocal.getAllSlaveAcceptanceConditions();
            for (Map.Entry<Map<GOperator, Integer>, TranSet<ProductState>> entry : accLocal.computeAccMasterOptions().entrySet()) {
                Map<GOperator, Integer> ranking = entry.getKey();
                Set<GOperator> gSet = ranking.keySet();

                TranSet<ProductState> Fin = new TranSet<>(factory);
                List<TranSet<ProductState>> Infs = new ArrayList<>();
                Fin.addAll(entry.getValue());

                for (GOperator g : gSet) {
                    Set<GOperator> localGSet = new HashSet<>(gSet);
                    localGSet.retainAll(accLocal.topmostGs.get(g));
                    RabinPair<ProductState> gPair;
                    gPair = completeSlaveAcceptance.get(g).get(localGSet).get(ranking.get(g));

                    Fin.addAll(gPair.fin);
                    Infs.add(gPair.inf.clone());
                }

                GeneralizedRabinPair<ProductState> pair = new GeneralizedRabinPair<>(Fin, Infs);
                accTGRRaw.add(pair);
            }

            return accTGRRaw;
        }

        public static <S extends IState<S>> void removeRedundancy(AccTGRRaw<S> this2) {
            List<GeneralizedRabinPair<S>> temp;
            List<TranSet<S>> copy;
            int phase = 0;
            Main.stopwatchLocal();

            Main.verboseln(phase + ". Raw Generalized Rabin Acceptance Condition\n");
            printProgress(phase++, this2);

            // This rule is subsumed by the following two rules.
            Main.verboseln(phase + ". Removing (F, {I1,...,In}) with complete F\n");
            this2.removeIf(pair -> this2.product.inputContainsAllAutomatonTransitions(pair.fin));
            printProgress(phase++, this2);

            Main.verboseln(phase + ". Removing F from each Ii: (F, {I1,...,In}) |-> (F, {I1\\F,...,In\\F})\n");
            this2.forEach(pair -> pair.infs.forEach(inf -> inf.removeAll(pair.fin)));
            printProgress(phase++, this2);

            Main.verboseln(phase + ". Removing (F, {..., \\emptyset, ...} )\n");
            this2.removeIf(pair -> pair.infs.stream().anyMatch(TranSet::isEmpty));
            printProgress(phase++, this2);

            Main.verboseln(phase + ". Removing complete Ii in (F, {I1,...,In}), i.e. Ii U F = Q \n");
            this2.forEach(pair -> pair.infs.removeIf(i ->
            this2.product.inputContainsAllAutomatonTransitions(i.union(pair.fin))));
            printProgress(phase++, this2);

            Main.verboseln(phase + ". Removing redundant Ii: (F, I) |-> (F, { i | i in I and !\\exists j in I : Ij <= Ii })\n");
            temp = new ArrayList<>();
            for (GeneralizedRabinPair<S> pair : this2) {
                copy = new ArrayList<>(pair.infs);
                for (TranSet<S> i : pair.infs) {
                    for (TranSet<S> j : pair.infs) {
                        if (!j.equals(i) && i.containsAll(j)) {
                            copy.remove(i);
                            break;
                        }
                    }
                }
                temp.add(new GeneralizedRabinPair<>(pair.fin, copy));
            }
            this2.clear();
            this2.addAll(temp);

            printProgress(phase++, this2);

            Main.verboseln(phase + ". Removing (F, I) for which there is a less restrictive (G, J) \n");
            Set<GeneralizedRabinPair<S>> hs = new HashSet<>(this2);


            for (GeneralizedRabinPair<S> pair1 : this2) {
                for (GeneralizedRabinPair<S> pair2 : this2) {
                    if (pair1.equals(pair2)) {
                        continue;
                    }

                    if (pair2.implies(pair1) && hs.contains(pair1)) {
                        hs.remove(pair2);
                        break;
                    }
                }
            }

            this2.clear();
            this2.addAll(hs);

            printProgress(phase, this2);
        }

        public static void printProgress(int phase, Collection<?> this2) {
            Main.nonsilent("Phase: " + phase + "..." + this2.size() + " pairs");
        }

        public static <S extends IState<S>> void removeRedundancyLightAfterEmptyCheck(AccTGRRaw<S> accTGR) {

            Iterator<GeneralizedRabinPair<S>> i = accTGR.iterator();
            while (i.hasNext()) {
                GeneralizedRabinPair<S> p = i.next();
                Set<TranSet<S>> s = new HashSet<>(p.infs);
                p.infs.clear();
                p.infs.addAll(s);
                if (p.infs.stream().anyMatch(TranSet::isEmpty)) {
                    i.remove();
                }
            }

            Set<GeneralizedRabinPair<S>> hs = new HashSet<>(accTGR);
            for (GeneralizedRabinPair<S> pair1 : accTGR) {
                for (GeneralizedRabinPair<S> pair2 : accTGR) {
                    if (pair1.equals(pair2)) {
                        continue;
                    }

                    if (pair2.implies(pair1) && hs.contains(pair1)) {
                        hs.remove(pair2);
                        break;
                    }
                }
            }
            accTGR.clear();
            accTGR.addAll(hs);
        }

        @Override
        public String toString() {
            String result = "Gen. Rabin acceptance condition";
            int i = 1;
            for (GeneralizedRabinPair<S> pair : this) {
                result += "\nPair " + i + "\n" + pair;
                i++;
            }
            return result;
        }
    }
}
