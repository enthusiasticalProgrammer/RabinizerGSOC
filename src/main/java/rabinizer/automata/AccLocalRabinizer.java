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

import ltl.UnaryModalOperator;
import ltl.equivalence.EquivalenceClassFactory;
import omega_automaton.collections.TranSet;
import omega_automaton.collections.Tuple;
import omega_automaton.collections.valuationset.ValuationSetFactory;
import rabinizer.automata.MojmirSlave.State;
import rabinizer.frequencyLTL.SlaveSubformulaVisitor;

class AccLocalRabinizer extends AccLocal<Map<UnaryModalOperator, Integer>, TranSet<Product<RabinSlave.State>.ProductState>, Map<Integer, Tuple<TranSet<Product<RabinSlave.State>.ProductState>, TranSet<Product<RabinSlave.State>.ProductState>>>, RabinSlave.State, ProductRabinizer> {

    private final Map<UnaryModalOperator, Integer> maxRank = new HashMap<>();

    public AccLocalRabinizer(ProductRabinizer product, ValuationSetFactory valuationSetFactory,
            EquivalenceClassFactory equivalenceFactory, Collection<Optimisation> opts) {
        super(product, valuationSetFactory, equivalenceFactory, opts);
        for (UnaryModalOperator gOperator : getOverallFormula().accept(new SlaveSubformulaVisitor())) {
            initialiseMaxRankOfSlaveOperator(gOperator);
        }
    }

    private final void initialiseMaxRankOfSlaveOperator(UnaryModalOperator UnarymodalOperator) {
        int maxRankF = 0;
        for (AbstractSelfProductSlave<?>.State rs : product.getSecondaryAutomata().get(UnarymodalOperator).getStates()) {
            maxRankF = Math.max(maxRankF, rs.size());
        }
        maxRank.put(UnarymodalOperator, maxRankF);
    }

    @Override
    protected void computeAccMasterForASingleGSet(Set<UnaryModalOperator> gSet, Map<Map<UnaryModalOperator, Integer>, TranSet<Product<RabinSlave.State>.ProductState>> result) {
        for (Map<UnaryModalOperator, Integer> ranking : powersetRanks(new ArrayDeque<>(gSet))) {

            TranSet<Product<RabinSlave.State>.ProductState> avoidP = new TranSet<>(valuationSetFactory);

            for (Product<RabinSlave.State>.ProductState ps : product.getStates()) {
                avoidP.addAll(computeNonAccMasterTransForState(ranking, ps));
            }

            if (!product.containsAllTransitions(avoidP)) {
                result.put(ImmutableMap.copyOf(ranking), avoidP);
            }
        }
    }

    private final Collection<Map<UnaryModalOperator, Integer>> powersetRanks(Deque<UnaryModalOperator> gSet) {
        UnaryModalOperator next = gSet.pollLast();

        if (next == null) {
            return Collections.singleton(Collections.emptyMap());
        }

        Collection<Map<UnaryModalOperator, Integer>> result = new ArrayList<>();

        for (Map<UnaryModalOperator, Integer> ranking : powersetRanks(gSet)) {
            for (int rank = 1; rank <= maxRank.get(next); rank++) {
                Map<UnaryModalOperator, Integer> rankingNew = new HashMap<>(ranking);
                rankingNew.put(next, rank);
                result.add(rankingNew);
            }
        }

        return result;
    }

    @Override
    protected Map<Integer, Tuple<TranSet<Product<RabinSlave.State>.ProductState>, TranSet<Product<RabinSlave.State>.ProductState>>> getSingleSlaveAccCond(UnaryModalOperator g,
            Set<State> finalStates) {
        Map<Integer, Tuple<TranSet<Product<RabinSlave.State>.ProductState>, TranSet<Product<RabinSlave.State>.ProductState>>> result = new HashMap<>();
        for (int rank = 1; rank <= maxRank.get(g); rank++) {
            result.put(rank, product.createRabinPair(product.secondaryAutomata.get(g), finalStates, rank));
        }
        return result;
    }
}
