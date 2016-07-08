package rabinizer.automata;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import ltl.FrequencyG;
import ltl.GOperator;
import ltl.UnaryModalOperator;
import ltl.equivalence.EquivalenceClassFactory;
import omega_automaton.collections.TranSet;
import omega_automaton.collections.valuationset.ValuationSetFactory;
import rabinizer.automata.MojmirSlave.State;
import rabinizer.automata.Product.ProductState;
import rabinizer.frequencyLTL.FOperatorForMojmir;

public class AccLocalControllerSynthesis extends AccLocal<Set<UnaryModalOperator>, TranSet<Product.ProductState<?>>, Map<TranSet<Product.ProductState<?>>, Integer>, ProductControllerSynthesis> {

    public AccLocalControllerSynthesis(ProductControllerSynthesis product, ValuationSetFactory valuationSetFactory, EquivalenceClassFactory equivalenceFactory,
            Collection<Optimisation> opts) {
        super(product, valuationSetFactory, equivalenceFactory, opts);
    }

    @Override
    protected void computeAccMasterForASingleGSet(Set<UnaryModalOperator> gSet, Map<Set<UnaryModalOperator>, TranSet<Product.ProductState<?>>> result) {

        TranSet<Product.ProductState<?>> avoidP = new TranSet<>(valuationSetFactory);

        for (Product.ProductState<?> ps : product.getStates()) {
            avoidP.addAll(computeNonAccMasterTransForStateIgoringRankings(gSet, ps));
        }

        if (!product.containsAllTransitions(avoidP)) {
            result.put(ImmutableSet.copyOf(gSet), avoidP);
        }
    }

    @Override
    protected Map<TranSet<ProductState<?>>, Integer> getSingleSlaveAccCond(UnaryModalOperator g, Set<State> finalStates) {
        if (g instanceof FrequencyG) {
            return product.getControllerAcceptanceFrequencyG((FrequencyG) g, finalStates);
        } else if (g instanceof GOperator) {
            Map<TranSet<ProductState<?>>, Integer> result = new HashMap<>();
            result.put(product.getControllerAcceptanceG((GOperator) g, finalStates), 0);
            return result;
        } else if (g instanceof FOperatorForMojmir) {
            Map<TranSet<ProductState<?>>, Integer> result = new HashMap<>();
            result.put(product.getControllerAcceptanceF((FOperatorForMojmir) g, finalStates), 0);
            return result;
        }
        throw new IllegalArgumentException("Formula is not a valid label of slave automata.");
    }
}
