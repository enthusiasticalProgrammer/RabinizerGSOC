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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jhoafparser.consumer.HOAConsumerPrint;
import ltl.Formula;
import ltl.UnaryModalOperator;
import ltl.equivalence.EquivalenceClassFactory;
import omega_automaton.collections.TranSet;
import omega_automaton.collections.Tuple;
import omega_automaton.collections.valuationset.ValuationSetFactory;
import rabinizer.exec.Main;
import rabinizer.frequencyLTL.SlaveSubformulaVisitor;
import rabinizer.automata.Optimisation;
import rabinizer.automata.Product.ProductState;

/**
 * This class contains the behaviour, which belongs to both constructions:
 * rabinizer and controller synthesis for MDPs and Frequency LTL \ GU.
 *
 * @param <T>
 *            type of self product slaves
 * @param <P>
 *            type of overall product
 */
public abstract class AbstractAutomatonFactory<T extends AbstractSelfProductSlave<?>, ParamProduct extends AbstractSelfProductSlave<ParamProduct>.State, P extends Product<ParamProduct>> {
    protected final Formula phi;
    protected final EquivalenceClassFactory equivalenceClassFactory;
    protected final ValuationSetFactory valuationSetFactory;
    protected final Collection<Optimisation> opts;
    protected P product;

    protected AbstractAutomatonFactory(Formula phi, EquivalenceClassFactory equivalenceClassFactory, ValuationSetFactory valuationSetFactory, Collection<Optimisation> opts) {
        this.phi = phi;
        this.equivalenceClassFactory = equivalenceClassFactory;
        this.valuationSetFactory = valuationSetFactory;
        this.opts = opts;
    }

    public final P constructAutomaton() {
        Master master = constructMaster();

        Map<UnaryModalOperator, T> slaves = constructSlaves();

        Main.nonsilent("========================================");
        Main.nonsilent("Generating product\n");

        product = obtainProduct(master, slaves);
        product.generate();

        if (!Main.isSilent()) {
            HOAConsumerPrint hoa = new HOAConsumerPrint(System.out);
            product.toHOA(hoa, null);
        }

        if (opts.contains(Optimisation.COMPUTE_ACC_CONDITION)) {
            Main.nonsilent("========================================");
            Main.nonsilent("Generating acceptance condition\n");

            constructAcceptance();

            Main.nonsilent("========================================");
            Main.nonsilent("Remove some redundancy of Acceptance Condition\n");

            removeRedundancy();

            Main.nonsilent("========================================");
            Main.nonsilent("Doing post-processing optimisations\n");

            doPostProcessingOptimisations();
        }
        return product;

    }

    protected final void removeRedundancy() {
        List<TranSet<Product<ParamProduct>.ProductState>> copy;
        Collection<Tuple<TranSet<Product<ParamProduct>.ProductState>, List<TranSet<Product<ParamProduct>.ProductState>>>> toRemove = new HashSet<>();

        product.getAcceptance().acceptanceCondition.stream().filter(pair -> product.containsAllTransitions(pair.left)).forEach(s -> toRemove.add(s));
        product.getAcceptance().remove(toRemove);
        toRemove.clear();

        product.getAcceptance().acceptanceCondition.forEach(pair -> pair.right.forEach(inf -> inf.removeAll(pair.left)));

        product.getAcceptance().acceptanceCondition.stream().filter(pair -> pair.right.stream().anyMatch(TranSet::isEmpty)).forEach(s -> toRemove.add(s));
        product.getAcceptance().remove(toRemove);
        toRemove.clear();

        product.getAcceptance().acceptanceCondition.forEach(pair -> pair.right.removeIf(i -> product.containsAllTransitions(i.union(pair.left))));

        Collection<Tuple<TranSet<Product<ParamProduct>.ProductState>, List<TranSet<Product<ParamProduct>.ProductState>>>> temp = new ArrayList<>();
        for (Tuple<TranSet<Product<ParamProduct>.ProductState>, List<TranSet<Product<ParamProduct>.ProductState>>> pair : product.getAcceptance().acceptanceCondition) {
            copy = new ArrayList<>(pair.right);
            for (TranSet<Product<ParamProduct>.ProductState> i : pair.right) {
                for (TranSet<Product<ParamProduct>.ProductState> j : pair.right) {
                    if (!j.equals(i) && i.containsAll(j)) {
                        copy.remove(i);
                        break;
                    }
                }
            }
            temp.add(new Tuple<>(pair.left, copy));
        }
        product.getAcceptance().acceptanceCondition.clear();
        product.getAcceptance().acceptanceCondition.addAll(temp);

        for (Tuple<TranSet<Product<ParamProduct>.ProductState>, List<TranSet<Product<ParamProduct>.ProductState>>> pair1 : product.getAcceptance().acceptanceCondition) {
            for (Tuple<TranSet<Product<ParamProduct>.ProductState>, List<TranSet<Product<ParamProduct>.ProductState>>> pair2 : product.getAcceptance().acceptanceCondition) {
                if (pair1.equals(pair2)) {
                    continue;
                }

                if (product.getAcceptance().implies(pair2, pair1) && !toRemove.contains(pair1)) {
                    toRemove.add(pair2);
                    break;
                }
            }
        }

        product.getAcceptance().remove(toRemove);
    }


    protected abstract P obtainProduct(Master master, Map<UnaryModalOperator, T> slaves);

    protected abstract void doPostProcessingOptimisations();

    protected abstract void constructAcceptance();

    final Master constructMaster() {
        Main.nonsilent("========================================");
        Main.nonsilent("Generating primaryAutomaton:\n");
        Master master = new Master(phi, equivalenceClassFactory, valuationSetFactory, opts);
        master.generate();
        if (!Main.isSilent()) {
            HOAConsumerPrint hoa = new HOAConsumerPrint(System.out);
            master.toHOA(hoa, null);
        }
        return master;
    }

    private final Map<UnaryModalOperator, T> constructSlaves() {
        Set<UnaryModalOperator> gSubformulas = phi.accept(new SlaveSubformulaVisitor());
        Map<UnaryModalOperator, T> slaves = new HashMap<>();

        for (UnaryModalOperator f : gSubformulas) {

            MojmirSlave mSlave = new MojmirSlave(f, equivalenceClassFactory, valuationSetFactory, opts);
            mSlave.generate();

            if (Main.isVerbose()) {
                HOAConsumerPrint hoa = new HOAConsumerPrint(System.out);
                Main.verboseln("Mojmir Slave: ");
                mSlave.toHOA(hoa, null);
            }

            T rSlave = obtainSelfProductSlave(mSlave);
            rSlave.generate();

            optimizeInitialStateOfSelfProductSlave(rSlave);

            slaves.put(f, rSlave);

            if (Main.isVerbose()) {
                HOAConsumerPrint hoa = new HOAConsumerPrint(System.out);
                Main.verboseln("\nRabin Slave: ");
                rSlave.toHOA(hoa, null);
            }
        }
        return slaves;
    }

    protected abstract void optimizeInitialStateOfSelfProductSlave(T rSlave);

    protected abstract T obtainSelfProductSlave(MojmirSlave mSlave);
}
