package rabinizer.automata;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;

import ltl.ModalOperator;
import ltl.equivalence.EquivalenceClassFactory;
import omega_automaton.collections.TranSet;
import omega_automaton.collections.Tuple;
import omega_automaton.collections.valuationset.ValuationSetFactory;
import rabinizer.automata.MojmirSlave.State;
import rabinizer.frequencyLTL.SlaveSubformulaVisitor;

public class AccLocalRabinizer extends
AccLocal<Map<ModalOperator, Integer>, TranSet<Product.ProductState<?>>, Map<Integer, Tuple<TranSet<Product.ProductState<?>>, TranSet<Product.ProductState<?>>>>, ProductRabinizer> {

    private final Map<ModalOperator, Integer> maxRank = new HashMap<>();

    public AccLocalRabinizer(ProductRabinizer product, ValuationSetFactory valuationSetFactory,
            EquivalenceClassFactory equivalenceFactory, Collection<Optimisation> opts) {
        super(product, valuationSetFactory, equivalenceFactory, opts);
        for (ModalOperator gOperator : getOverallFormula().accept(new SlaveSubformulaVisitor())) {
            initialiseMaxRankOfSlaveOperator(gOperator);
        }
    }

    private final void initialiseMaxRankOfSlaveOperator(ModalOperator modalOperator) {
        int maxRankF = 0;
        for (AbstractSelfProductSlave<?>.State rs : product.getSecondaryAutomata().get(modalOperator).getStates()) {
            maxRankF = Math.max(maxRankF, rs.size());
        }
        maxRank.put(modalOperator, maxRankF);
    }

    @Override
    protected void computeAccMasterForASingleGSet(Set<ModalOperator> gSet, Map<Map<ModalOperator, Integer>, TranSet<ProductRabinizer.ProductState<?>>> result) {
        for (Map<ModalOperator, Integer> ranking : powersetRanks(new ArrayDeque<>(gSet))) {

            TranSet<Product.ProductState<?>> avoidP = new TranSet<>(valuationSetFactory);

            for (Product.ProductState<?> ps : product.getStates()) {
                avoidP.addAll(computeNonAccMasterTransForState(ranking, ps));
            }

            if (!product.containsAllTransitions(avoidP)) {
                result.put(ImmutableMap.copyOf(ranking), avoidP);
            }
        }
    }

    private final Collection<Map<ModalOperator, Integer>> powersetRanks(Deque<ModalOperator> gSet) {
        ModalOperator next = gSet.pollLast();

        if (next == null) {
            return Collections.singleton(Collections.emptyMap());
        }

        Collection<Map<ModalOperator, Integer>> result = new ArrayList<>();

        for (Map<ModalOperator, Integer> ranking : powersetRanks(gSet)) {
            for (int rank = 1; rank <= maxRank.get(next); rank++) {
                Map<ModalOperator, Integer> rankingNew = new HashMap<>(ranking);
                rankingNew.put(next, rank);
                result.add(rankingNew);
            }
        }

        return result;
    }

    @Override
    protected Map<Integer, Tuple<TranSet<Product.ProductState<?>>, TranSet<Product.ProductState<?>>>> getSingleSlaveAccCond(ModalOperator g, Set<State> finalStates) {
        Map<Integer, Tuple<TranSet<Product.ProductState<?>>, TranSet<Product.ProductState<?>>>> result = new HashMap<>();
        for (int rank = 1; rank <= maxRank.get(g); rank++) {
            result.put(rank, product.createRabinPair(product.secondaryAutomata.get(g), finalStates, rank));
        }
        return result;
    }
}
