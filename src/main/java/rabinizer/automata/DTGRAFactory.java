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
import omega_automaton.acceptance.GeneralisedRabinAcceptance;
import omega_automaton.collections.TranSet;
import omega_automaton.collections.Tuple;
import omega_automaton.collections.valuationset.ValuationSetFactory;
import ltl.Formula;
import ltl.ModalOperator;
import ltl.equivalence.EquivalenceClassFactory;

import java.util.*;
import java.util.Map.Entry;

public class DTGRAFactory extends AbstractAutomatonFactory<RabinSlave, ProductRabinizer> {

    public DTGRAFactory(Formula phi, EquivalenceClassFactory equivalenceClassFactory, ValuationSetFactory valuationSetFactory, Collection<Optimisation> opts) {
        super(phi, equivalenceClassFactory, valuationSetFactory, opts);
    }

    @Override
    protected void constructAcceptance() {
        GeneralisedRabinAcceptance<ProductState<?>> result = new GeneralisedRabinAcceptance<>(new ArrayList<>());
        AccLocalRabinizer accLocal = new AccLocalRabinizer(product, valuationSetFactory, equivalenceClassFactory, opts);

        Map<ModalOperator, Map<Set<ModalOperator>, Map<Integer, Tuple<TranSet<ProductState<?>>, TranSet<ProductState<?>>>>>> completeSlaveAcceptance = accLocal
                .getAllSlaveAcceptanceConditions();
        for (Entry<Map<ModalOperator, Integer>, TranSet<ProductState<?>>> entry : accLocal.computeAccMasterOptions().entrySet()) {
            Map<ModalOperator, Integer> ranking = entry.getKey();
            Set<ModalOperator> gSet = ranking.keySet();

            TranSet<ProductState<?>> Fin = new TranSet<>(valuationSetFactory);
            List<TranSet<ProductState<?>>> Infs = new ArrayList<>();
            Fin.addAll(entry.getValue());

            for (ModalOperator g : gSet) {
                Set<ModalOperator> localGSet = new HashSet<>(gSet);
                localGSet.retainAll(accLocal.topmostSlaves.get(g));
                Tuple<TranSet<ProductState<?>>, TranSet<ProductState<?>>> gPair;
                gPair = completeSlaveAcceptance.get(g).get(localGSet).get(ranking.get(g));

                Fin.addAll(gPair.left);
                Infs.add(gPair.right.clone());
            }
            result.acceptanceCondition.add(new Tuple<>(Fin, Infs));
        }

        product.setAcceptance(result);
    }

    @Override
    protected void doPostProcessingOptimisations() {
        /**
         * Side effect: empty sink-SCCs get deleted, acceptance condition gets
         * reduced when possible
         *
         * @return true if automaton together witch acceptance condition is
         *         empty
         */
        if (opts.contains(Optimisation.EMPTINESS_CHECK)) {
            EmptinessCheck.checkEmptinessAndMinimiseSCCBasedProduct(product);
            removeRedundancyLightAfterEmptinessCheck();
        }
    }

    public void removeRedundancyLightAfterEmptinessCheck() {
        Collection<Tuple<TranSet<ProductState<?>>, List<TranSet<ProductState<?>>>>> toRemove = new HashSet<>();
        for (Tuple<TranSet<ProductState<?>>, List<TranSet<ProductState<?>>>> pair : product.getAcceptance().acceptanceCondition) {
            if (pair.right.stream().anyMatch(TranSet::isEmpty)) {
                toRemove.add(pair);
            }
        }
        product.getAcceptance().acceptanceCondition.removeAll(toRemove);

        toRemove.clear();
        for (Tuple<TranSet<ProductState<?>>, List<TranSet<ProductState<?>>>> pair1 : product.getAcceptance().acceptanceCondition) {
            for (Tuple<TranSet<ProductState<?>>, List<TranSet<ProductState<?>>>> pair2 : product.getAcceptance().acceptanceCondition) {
                if (pair1.equals(pair2)) {
                    continue;
                }

                if (product.getAcceptance().implies(pair2, pair1) && !toRemove.contains(pair1)) {
                    toRemove.add(pair2);
                    break;
                }
            }
        }
        product.getAcceptance().acceptanceCondition.removeAll(toRemove);
    }

    @Override
    protected ProductRabinizer obtainProduct(Master master, Map<ModalOperator, RabinSlave> slaves) {
        return new ProductRabinizer(master, slaves, valuationSetFactory, opts);
    }

    @Override
    protected void optimizeInitialStateOfSelfProductSlave(RabinSlave rSlave) {
        if (opts.contains(Optimisation.OPTIMISE_INITIAL_STATE)) {
            rSlave.optimizeInitialState();
        }
    }

    @Override
    protected RabinSlave obtainSelfProductSlave(MojmirSlave mSlave) {
        return new RabinSlave(mSlave, valuationSetFactory);
    }
}
