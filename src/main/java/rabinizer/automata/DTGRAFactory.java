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

import omega_automaton.acceptance.GeneralisedRabinAcceptance;
import omega_automaton.collections.TranSet;
import omega_automaton.collections.Tuple;
import omega_automaton.collections.valuationset.ValuationSetFactory;
import ltl.Formula;
import ltl.UnaryModalOperator;
import ltl.equivalence.EquivalenceClassFactory;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.IntStream;

public class DTGRAFactory extends AbstractAutomatonFactory<RabinSlave, RabinSlave.State, ProductRabinizer> {

    public DTGRAFactory(Formula phi, EquivalenceClassFactory equivalenceClassFactory, ValuationSetFactory valuationSetFactory, Collection<Optimisation> opts) {
        super(phi, equivalenceClassFactory, valuationSetFactory, opts);
    }

    @Override
    protected void constructAcceptance() {
        GeneralisedRabinAcceptance<Product<RabinSlave.State>.ProductState> result = new GeneralisedRabinAcceptance<>(new ArrayList<>());
        AccLocalRabinizer accLocal = new AccLocalRabinizer(product, valuationSetFactory, equivalenceClassFactory, opts);

        Map<UnaryModalOperator, Map<Set<UnaryModalOperator>, Map<Integer, Tuple<TranSet<Product<RabinSlave.State>.ProductState>, TranSet<Product<RabinSlave.State>.ProductState>>>>> completeSlaveAcceptance = accLocal
                .getAllSlaveAcceptanceConditions();
        for (Entry<Map<UnaryModalOperator, Integer>, TranSet<Product<RabinSlave.State>.ProductState>> entry : accLocal.computeAccMasterOptions().entrySet()) {
            Map<UnaryModalOperator, Integer> ranking = entry.getKey();
            Set<UnaryModalOperator> gSet = ranking.keySet();

            TranSet<Product<RabinSlave.State>.ProductState> Fin = new TranSet<>(valuationSetFactory);
            List<TranSet<Product<RabinSlave.State>.ProductState>> Infs = new ArrayList<>();
            Fin.addAll(entry.getValue());

            for (UnaryModalOperator g : gSet) {
                Set<UnaryModalOperator> localGSet = new HashSet<>(gSet);
                localGSet.retainAll(accLocal.topmostSlaves.get(g));
                Tuple<TranSet<Product<RabinSlave.State>.ProductState>, TranSet<Product<RabinSlave.State>.ProductState>> gPair;
                gPair = completeSlaveAcceptance.get(g).get(localGSet).get(ranking.get(g));

                Fin.addAll(gPair.left);
                Infs.add(gPair.right.copy());
            }
            result.addPair(new Tuple<>(Fin, Infs));
        }

        product.setAcceptance(result);
    }

    /**
     * Side effect: empty sink-SCCs get deleted, acceptance condition gets
     * reduced when possible
     *
     * @return true if automaton together witch acceptance condition is
     *         empty
     */
    @Override
    protected void doPostProcessingOptimisations() {
        if (opts.contains(Optimisation.EMPTINESS_CHECK)) {
            EmptinessCheck.checkEmptinessAndMinimiseSCCBasedProduct(product);
            removeRedundancyLightAfterEmptinessCheck();
        }
    }

    public void removeRedundancyLightAfterEmptinessCheck() {
        Set<Integer> toRemove = new HashSet<>();
        IntStream.range(0, product.getAcceptance().unmodifiableCopyOfAcceptanceCondition().size())
        .filter(i -> product.getAcceptance().unmodifiableCopyOfAcceptanceCondition().get(i).right.stream().anyMatch(x -> x.isEmpty())).forEach(toRemove::add);
        product.getAcceptance().removeIndices(toRemove);
        toRemove.clear();

        toRemove.clear();
        for (int i = 0; i < product.getAcceptance().unmodifiableCopyOfAcceptanceCondition().size(); i++) {
            for (int j = 0; j < product.getAcceptance().unmodifiableCopyOfAcceptanceCondition().size(); j++) {
                if (i == j) {
                    continue;
                }

                if (product.getAcceptance().implies(i, j) && !toRemove.contains(j)) {
                    toRemove.add(i);
                    break;
                }
            }
        }
        product.getAcceptance().removeIndices(toRemove);
    }

    @Override
    protected ProductRabinizer obtainProduct(Master master, Map<UnaryModalOperator, RabinSlave> slaves) {
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
