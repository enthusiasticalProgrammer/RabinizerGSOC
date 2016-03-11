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

public class DTGRAFactory {

    private DTGRAFactory() {

    }

    public static DTGRA constructDTGRA(Formula phi, EquivalenceClassFactory equivalenceClassFactory, ValuationSetFactory valuationSetFactory, Collection<Optimisation> opts) {
        Main.verboseln("========================================");
        Main.nonsilent("Generating primaryAutomaton");
        Master master;

        if (opts.contains(Optimisation.SLAVE_SUSPENSION)) {
            master = new SuspendedMaster(phi, equivalenceClassFactory, valuationSetFactory, opts);
        } else {
            master = new Master(phi, equivalenceClassFactory, valuationSetFactory, opts, true);
        }
        master.generate();

        Main.verboseln("========================================");
        Main.nonsilent("Generating Mojmir & Rabin secondaryAutomata");
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
        }

        Main.verboseln("========================================");
        Main.nonsilent("Generating product");

        Product automaton = new Product(master, slaves, valuationSetFactory, opts);
        automaton.generate();

        AccTGRRaw<ProductState> accTGR = null;

        if (opts.contains(Optimisation.COMPUTE_ACC_CONDITION)) {
            Main.verboseln("========================================");
            Main.nonsilent("Generating local acceptance conditions");

            AccLocal accLocal = new AccLocal(automaton, valuationSetFactory, equivalenceClassFactory, opts);

            Main.verboseln("========================================");
            Main.nonsilent("Generating global acceptance condition");
            accTGR = AccTGRRaw.createAccTGRRaw(accLocal, valuationSetFactory);

            Main.nonsilent("Generating optimized acceptance condition");
            accTGR.removeRedundancy();
            Main.verboseln("========================================");

            /**
             * Side effect: empty sink-SCCs get deleted, acceptance condition gets
             * reduced when possible
             *
             * @return true if automaton together witch acceptance condition is empty
             */
            if (opts.contains(Optimisation.EMPTINESS_CHECK)) {
                // if it is empty, we have to complete it
                if (EmptinessCheck.checkEmptiness(automaton, accTGR)) {
                    opts.add(Optimisation.COMPLETE);
                }
            }

            if (opts.contains(Optimisation.COMPLETE)) {
                automaton.makeComplete();

                if (automaton.states.contains(automaton.trapState)) {
                    for (GeneralizedRabinPair<ProductState> rabPair : accTGR) {
                        rabPair.fin.addAll(automaton.trapState, automaton.valuationSetFactory.createUniverseValuationSet());
                    }
                }
            }
        }

        return new DTGRA(automaton, accTGR);
    }

    public static class AccTGRRaw<S extends IState<S>> extends LinkedList<GeneralizedRabinPair<S>> {

        private static final long serialVersionUID = 245172601429256815L;
        protected final ValuationSetFactory valuationSetFactory;
        private final TranSet<S> allTrans;

        private AccTGRRaw(TranSet<S> allTrans, ValuationSetFactory factory) {
            this.allTrans = allTrans;
            this.valuationSetFactory = factory;
        }

        public static AccTGRRaw<ProductState> createAccTGRRaw(AccLocal accLocal, ValuationSetFactory factory) {
            AccTGRRaw<ProductState> accTGRRaw = new AccTGRRaw<>(accLocal.allTrans, factory);

            for (Map.Entry<Map<GOperator, Integer>, TranSet<ProductState>> entry : accLocal.accMasterOptions.entrySet()) {
                Map<GOperator, Integer> ranking = entry.getKey();
                Set<GOperator> gSet = ranking.keySet();

                Main.verboseln("\tGSet " + gSet);
                Main.verboseln("\t  Ranking " + ranking);

                TranSet<ProductState> Fin = new TranSet<>(factory);
                List<TranSet<ProductState>> Infs = new ArrayList<>();
                Fin.addAll(entry.getValue());

                for (GOperator g : gSet) {
                    Set<GOperator> localGSet = new HashSet<>(gSet);
                    localGSet.retainAll(accLocal.topmostGs.get(g));
                    RabinPair<ProductState> fPair;

                    if (accLocal.accSlavesOptions.get(g).get(localGSet) != null) {
                        fPair = accLocal.accSlavesOptions.get(g).get(localGSet).get(ranking.get(g));
                    } else {
                        fPair = accLocal.computeAccSlavesOptions(g, true).get(localGSet).get(ranking.get(g));
                    }

                    Fin.addAll(fPair.fin);
                    Infs.add((fPair.inf).clone());
                }

                GeneralizedRabinPair<ProductState> pair = new GeneralizedRabinPair<>(Fin, Infs);
                Main.verboseln(pair.toString());
                accTGRRaw.add(pair);
            }

            return accTGRRaw;
        }

        public void removeRedundancy() { // (TranSet<ProductState> allTrans) {
            List<GeneralizedRabinPair<S>> removalPairs;
            AccTGRRaw<S> temp;
            List<TranSet<S>> copy;
            int phase = 0;
            Main.stopwatchLocal();

            Main.verboseln(phase + ". Raw Generalized Rabin Acceptance Condition\n");
            printProgress(phase++);

            Main.verboseln(phase + ". Removing (F, {I1,...,In}) with complete F\n");
            removalPairs = new ArrayList<>();
            for (GeneralizedRabinPair<S> pair : this) {
                if (pair.fin.equals(allTrans)) {
                    removalPairs.add(pair);
                }
            }
            this.removeAll(removalPairs);
            printProgress(phase++);

            Main.verboseln(phase + ". Removing complete Ii in (F, {I1,...,In}), i.e. Ii U F = Q \n");
            temp = new AccTGRRaw<>(null, valuationSetFactory);
            for (GeneralizedRabinPair<S> pair : this) {
                copy = new ArrayList<>(pair.infs);

                for (TranSet<S> i : pair.infs) {
                    TranSet<S> iUf = new TranSet<>(valuationSetFactory);
                    iUf.addAll(i);
                    iUf.addAll(pair.fin);
                    if (iUf.equals(allTrans)) {
                        copy.remove(i);
                        break;
                    }
                }

                temp.add(new GeneralizedRabinPair<>(pair.fin, copy));
            }
            this.clear();
            this.addAll(temp);
            printProgress(phase++);

            Main.verboseln(phase + ". Removing F from each Ii: (F, {I1,...,In}) |-> (F, {I1\\F,...,In\\F})\n");
            temp = new AccTGRRaw<>(null, valuationSetFactory);
            for (GeneralizedRabinPair<S> pair : this) {
                copy = new ArrayList<>(pair.infs);
                for (TranSet<S> i : pair.infs) {
                    copy.remove(i); // System.out.println("101:::::::"+i);
                    TranSet<S> inew = new TranSet<>(valuationSetFactory);
                    inew.addAll(i); // System.out.println("105TEMP-BEFORE"+temp+"\n=====");
                    inew.removeAll(pair.fin); // System.out.println("105TEMP-BETWEEN"+temp+"\n=====");
                    copy.add(inew); // System.out.println("103TEMP-AFTER"+temp);
                }
                temp.add(new GeneralizedRabinPair<>(pair.fin, copy));// System.out.println("105TEMP-AFTER"+temp+"\n=====");
            }
            this.clear();
            this.addAll(temp);
            // Main.verboseln(this.toString());
            printProgress(phase++);

            Main.verboseln(phase + ". Removing (F, {..., \\emptyset, ...} )\n");
            removalPairs = new AccTGRRaw<>(null, valuationSetFactory);
            for (GeneralizedRabinPair<S> pair : this) {
                for (TranSet<S> i : pair.infs) {
                    if (i.isEmpty()) {
                        removalPairs.add(pair);
                        break;
                    }
                }
            }
            this.removeAll(removalPairs);
            // Main.verboseln(this.toString());
            printProgress(phase++);

            Main.verboseln(
                    phase + ". Removing redundant Ii: (F, I) |-> (F, { i | i in I and !\\exists j in I : Ij <= Ii })\n");
            for (GeneralizedRabinPair<S> pair : this) {
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
            this.clear();
            this.addAll(temp);

            // Main.verboseln(this.toString());
            printProgress(phase++);

            Main.verboseln(phase + ". Removing (F, I) for which there is a less restrictive (G, J) \n");
            removalPairs = new ArrayList<>();

            for (GeneralizedRabinPair<S> pair1 : this) {
                for (GeneralizedRabinPair<S> pair2 : this) {
                    if (pair1 == pair2) {
                        continue;
                    }

                    if (pair2.implies(pair1) && !removalPairs.contains(pair2)) {
                        removalPairs.add(pair1);
                        break;
                    }
                }
            }

            removeAll(removalPairs);

            // Main.verboseln(this.toString());
            printProgress(phase);
        }

        public void printProgress(int phase) {
            Main.nonsilent("Phase " + phase + ": " + Main.stopwatchLocal() + " s " + this.size() + " pairs");
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
