package rabinizer.automata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import ltl.Formula;
import ltl.FrequencyG;
import ltl.GOperator;
import ltl.UnaryModalOperator;
import ltl.equivalence.EquivalenceClassFactory;
import omega_automaton.collections.TranSet;
import omega_automaton.collections.Tuple;
import omega_automaton.collections.valuationset.ValuationSetFactory;
import rabinizer.DTGRMAAcceptance.BoundAndReward;
import rabinizer.DTGRMAAcceptance.GeneralisedRabinWithMeanPayoffAcceptance;
import rabinizer.automata.Product.ProductState;
import rabinizer.frequencyLTL.FOperatorForMojmir;

public class DTGRMAFactory extends AbstractAutomatonFactory<FrequencySelfProductSlave, FrequencySelfProductSlave.State, ProductControllerSynthesis> {

    public DTGRMAFactory(Formula phi, EquivalenceClassFactory equivalenceClassFactory, ValuationSetFactory valuationSetFactory, Collection<Optimisation> opts) {
        super(phi, equivalenceClassFactory, valuationSetFactory, opts);
    }

    @Override
    protected void constructAcceptance() {
        AccLocalControllerSynthesis accLocal = new AccLocalControllerSynthesis(product, valuationSetFactory, equivalenceClassFactory, opts);

        Map<UnaryModalOperator, Map<Set<UnaryModalOperator>, Map<TranSet<Product<FrequencySelfProductSlave.State>.ProductState>, Integer>>> completeSlaveAcceptance = accLocal
                .getAllSlaveAcceptanceConditions();

        List<Tuple<TranSet<Product<FrequencySelfProductSlave.State>.ProductState>, List<TranSet<Product<FrequencySelfProductSlave.State>.ProductState>>>> genRabinCondition = new ArrayList<>();
        List<Collection<BoundAndReward>> mdpCondition = new ArrayList<>();

        for (Entry<Set<UnaryModalOperator>, TranSet<Product<FrequencySelfProductSlave.State>.ProductState>> entry : accLocal.computeAccMasterOptions().entrySet()) {
            Set<UnaryModalOperator> gSet = entry.getKey();

            Map<FrequencyG, BoundAndReward> mdpAcceptance = new HashMap<>();

            TranSet<Product<FrequencySelfProductSlave.State>.ProductState> Fin = new TranSet<>(valuationSetFactory);
            List<TranSet<Product<FrequencySelfProductSlave.State>.ProductState>> Infs = new ArrayList<>();
            Fin.addAll(entry.getValue());

            for (UnaryModalOperator g : gSet) {
                Set<UnaryModalOperator> localGSet = new HashSet<>(gSet);
                localGSet.retainAll(accLocal.topmostSlaves.get(g));
                Map<TranSet<Product<FrequencySelfProductSlave.State>.ProductState>, Integer> singleAccCondition = completeSlaveAcceptance.get(g).get(localGSet);

                if (g instanceof FrequencyG) {
                    FrequencyG freqg = (FrequencyG) g;
                    BoundAndReward current = mdpAcceptance.get(freqg);
                    if (current == null) {
                        current = new BoundAndReward(freqg, valuationSetFactory);
                    }
                    current.increaseRewards(singleAccCondition);
                    mdpAcceptance.put(freqg, current);

                } else if (g instanceof FOperatorForMojmir) {
                    Infs.addAll(singleAccCondition.keySet());
                } else if (g instanceof GOperator) {
                    singleAccCondition.keySet().forEach(set -> Fin.addAll(set));
                } else {
                    throw new AssertionError();
                }
            }

            genRabinCondition.add(new Tuple<>(Fin, Infs));

            if (mdpAcceptance.values() == null) {
                mdpCondition.add(Collections.emptySet());
            } else {
                mdpCondition.add(mdpAcceptance.values());
            }
        }
        GeneralisedRabinWithMeanPayoffAcceptance acc = new GeneralisedRabinWithMeanPayoffAcceptance(genRabinCondition, mdpCondition);
        product.setAcceptance(acc);
    }

    @Override
    protected void doPostProcessingOptimisations() {
        // The post-processing optimisations include
        // currently the emptiness-check, which is not yet
        // generalised such that it can cope with
        // mean-payoff-conditions.
    }

    @Override
    protected ProductControllerSynthesis obtainProduct(Master master, Map<UnaryModalOperator, FrequencySelfProductSlave> slaves) {
        return new ProductControllerSynthesis(master, slaves, valuationSetFactory, opts);

    }

    @Override
    protected void optimizeInitialStateOfSelfProductSlave(FrequencySelfProductSlave rSlave) {
        // nothing to do, because this optimisation is not (yet) implemented
    }

    @Override
    protected FrequencySelfProductSlave obtainSelfProductSlave(MojmirSlave mSlave) {
        return new FrequencySelfProductSlave(mSlave, valuationSetFactory);
    }
}
