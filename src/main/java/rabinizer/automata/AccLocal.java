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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import rabinizer.collections.Collections3;
import rabinizer.collections.valuationset.ValuationSet;
import rabinizer.collections.valuationset.ValuationSetFactory;
import rabinizer.ltl.Conjunction;
import rabinizer.ltl.Formula;
import rabinizer.ltl.GOperator;
import rabinizer.ltl.SkeletonVisitor;
import rabinizer.ltl.equivalence.EquivalenceClass;
import rabinizer.ltl.equivalence.EquivalenceClassFactory;

import java.util.*;

class AccLocal {

    private final Product product;
    private final ValuationSetFactory valuationSetFactory;
    private final EquivalenceClassFactory equivalenceClassFactory;
    final Map<GOperator, Set<GOperator>> topmostGs = new HashMap<>();
    private final Map<GOperator, Integer> maxRank = new HashMap<>();
    private final Collection<Optimisation> optimisations;

    public AccLocal(Product product, ValuationSetFactory valuationSetFactory, EquivalenceClassFactory equivalenceFactory, Collection<Optimisation> opts) {
        this.product = product;
        this.valuationSetFactory = valuationSetFactory;
        this.equivalenceClassFactory = equivalenceFactory;
        optimisations = opts;

        for (GOperator gOperator : getOverallFormula().gSubformulas()) {
            initialiseMaxRankOfGOperator(gOperator);
            topmostGs.put(gOperator, gOperator.operand.topmostGs());
        }
    }

    private void initialiseMaxRankOfGOperator(GOperator gOperator) {
        int maxRankF = 0;
        for (RabinSlave.State rs : product.secondaryAutomata.get(gOperator).getStates()) {
            maxRankF = Math.max(maxRankF, rs.size());
        }
        maxRank.put(gOperator, maxRankF);
    }

    public Map<Map<GOperator, Integer>, TranSet<Product.ProductState>> computeAccMasterOptions() {
        Map<Map<GOperator, Integer>, TranSet<Product.ProductState>> result = new HashMap<>();

        Set<Set<GOperator>> gSets;

        if (optimisations.contains(Optimisation.SKELETON)) {
            gSets = getOverallFormula().accept(SkeletonVisitor.getInstance(SkeletonVisitor.SkeletonApproximation.LOWER_BOUND));
        } else {
            gSets = Sets.powerSet(getOverallFormula().gSubformulas());
        }

        for (Set<GOperator> gSet : gSets) {
            for (Map<GOperator, Integer> ranking : powersetRanks(new ArrayDeque<>(gSet))) {

                TranSet<Product.ProductState> avoidP = new TranSet<>(valuationSetFactory);

                for (Product.ProductState ps : product.getStates()) {
                    avoidP.addAll(computeNonAccMasterTransForState(ranking, ps));
                }

                if (!product.containsAllTransitions(avoidP)) {
                    result.put(ImmutableMap.copyOf(ranking), avoidP);
                }
            }
        }

        return result;
    }

    private TranSet<Product.ProductState> computeNonAccMasterTransForState(Map<GOperator, Integer> ranking, Product.ProductState ps) {
        TranSet<Product.ProductState> result = new TranSet<>(valuationSetFactory);

        if (optimisations.contains(Optimisation.EAGER)) {
            BitSet sensitiveAlphabet = ps.getSensitiveAlphabet();

            for (BitSet valuation : Collections3.powerSet(sensitiveAlphabet)) {
                if (!slavesEntail(ps, ranking, valuation, ps.primaryState.getClazz())) {
                    result.addAll(ps, valuationSetFactory.createValuationSet(valuation, sensitiveAlphabet));
                }
            }
        } else {
            if (!slavesEntail(ps, ranking, null, ps.primaryState.getClazz())) {
                result.addAll(ps, valuationSetFactory.createUniverseValuationSet());
            }
        }

        return result;
    }

    private Map<Set<GOperator>, Map<Integer, RabinPair<Product.ProductState>>> computeAccSlavesOptions(GOperator g) {
        Map<Set<GOperator>, Map<Integer, RabinPair<Product.ProductState>>> result = new HashMap<>();

        RabinSlave rSlave = product.secondaryAutomata.get(g);
        Set<Set<GOperator>> gSets = Sets.powerSet(getOverallFormula().gSubformulas());

        for (Set<GOperator> gSet : gSets) {
            Set<MojmirSlave.State> finalStates = new HashSet<>();
            EquivalenceClass gSetClazz = equivalenceClassFactory.createEquivalenceClass(new Conjunction(gSet));

            for (MojmirSlave.State fs : rSlave.mojmir.getStates()) {
                if (gSetClazz.implies(fs.getClazz())) {
                    finalStates.add(fs);
                }
            }

            result.put(gSet, new HashMap<>());
            for (int rank = 1; rank <= maxRank.get(g); rank++) {
                result.get(gSet).put(rank, createRabinPair(rSlave, finalStates, rank));
            }
        }

        return result;
    }

    public Map<GOperator, Map<Set<GOperator>, Map<Integer, RabinPair<Product.ProductState>>>> getAllSlaveAcceptanceConditions() {
        Map<GOperator, Map<Set<GOperator>, Map<Integer, RabinPair<Product.ProductState>>>> result = new HashMap<>();
        for (GOperator g : product.secondaryAutomata.keySet()) {
            result.put(g, computeAccSlavesOptions(g));
        }

        return result;
    }

    // TODO: Move to Product.State
    private RabinPair<Product.ProductState> createRabinPair(RabinSlave slave, Set<MojmirSlave.State> finalStates, int rank) {
        TranSet<Product.ProductState> failP = getFailingProductTransitions(slave, finalStates);
        TranSet<Product.ProductState> succeedP = getSucceedingProductTransitions(slave, rank, finalStates);
        TranSet<Product.ProductState> buyP = getBuyProductTransitions(slave, finalStates, rank);
        failP.addAll(buyP);
        return new RabinPair<>(failP, succeedP);
    }

    // TODO: Move to Product.State
    private TranSet<Product.ProductState> getBuyProductTransitions(RabinSlave slave, Set<MojmirSlave.State> finalStates, int rank) {
        TranSet<RabinSlave.State> buyR = getBuyRabinTransitions(slave, finalStates, rank);
        TranSet<Product.ProductState> buyP = new TranSet<>(valuationSetFactory);

        for (Product.ProductState ps : product.getStates()) {
            RabinSlave.State rs = ps.secondaryStates.get(slave.mojmir.label);
            if (rs != null) { // relevant slave
                buyP.addAll(ps, buyR.asMap().get(rs));
            }
        }

        return buyP;
    }

    // TODO: Move to Product.State
    private TranSet<RabinSlave.State> getBuyRabinTransitions(RabinSlave slave, Set<MojmirSlave.State> finalStates, int rank) {
        TranSet<RabinSlave.State> buyRabin = new TranSet<>(valuationSetFactory);
        for (RabinSlave.State rs : slave.getStates()) {
            for (Map.Entry<MojmirSlave.State, Integer> stateIntegerEntry : rs.entrySet()) {
                if (stateIntegerEntry.getValue() < rank) {
                    for (MojmirSlave.State fs2 : rs.keySet()) {
                        for (MojmirSlave.State succ : slave.mojmir.getStates()) {
                            ValuationSet vs1, vs2;
                            if (!finalStates.contains(succ) && (vs1 = slave.mojmir.transitions.get(stateIntegerEntry.getKey()).get(succ)) != null
                                    && (vs2 = slave.mojmir.transitions.get(fs2).get(succ)) != null) {
                                if (!stateIntegerEntry.getKey().equals(fs2)) {
                                    ValuationSet vs1copy = vs1.clone();
                                    vs1copy.retainAll(vs2);
                                    buyRabin.addAll(rs, vs1copy);
                                } else if (succ.equals(slave.mojmir.getInitialState())) {
                                    buyRabin.addAll(rs, vs1);
                                }

                            }
                        }
                    }
                }
            }
        }
        return buyRabin;
    }

    // TODO: Move to Product.State
    private TranSet<Product.ProductState> getSucceedingProductTransitions(RabinSlave slave, int rank, Set<MojmirSlave.State> finalStates) {
        TranSet<MojmirSlave.State> succeedMojmir = getSucceedingMojmirTransitions(slave, finalStates);
        TranSet<Product.ProductState> succeedP = new TranSet<>(valuationSetFactory);
        for (Product.ProductState ps : product.getStates()) {
            RabinSlave.State rs = ps.secondaryStates.get(slave.mojmir.label);
            if (rs != null) { // relevant slave
                for (Map.Entry<MojmirSlave.State, Integer> stateIntegerEntry : rs.entrySet()) {
                    if (stateIntegerEntry.getValue() == rank) {
                        succeedP.addAll(ps, succeedMojmir.asMap().get(stateIntegerEntry.getKey()));
                    }
                }
            }
        }
        return succeedP;
    }

    // TODO: Move to MojmirSlave.State
    private TranSet<MojmirSlave.State> getSucceedingMojmirTransitions(RabinSlave slave, Set<MojmirSlave.State> finalStates) {
        TranSet<MojmirSlave.State> succeedM = new TranSet<>(valuationSetFactory);
        if (finalStates.contains(slave.mojmir.getInitialState())) {
            succeedM.addAll(slave.mojmir.getAllTransitions());
        } else {
            for (MojmirSlave.State mojmirState : slave.mojmir.getStates()) {
                if (!finalStates.contains(mojmirState)) {
                    for (Map.Entry<MojmirSlave.State, ValuationSet> valuation : slave.mojmir.getSuccessors(mojmirState).entrySet()) {
                        if (finalStates.contains(valuation.getKey())) {
                            succeedM.addAll(mojmirState, valuation.getValue());
                        }
                    }
                }
            }
        }
        return succeedM;
    }

    // TODO: Move to Product.State
    private TranSet<Product.ProductState> getFailingProductTransitions(RabinSlave slave, Set<MojmirSlave.State> finalStates) {
        TranSet<MojmirSlave.State> failMojmir = getFailingMojmirTransitions(slave, finalStates);
        TranSet<Product.ProductState> failP = new TranSet<>(valuationSetFactory);
        for (Product.ProductState ps : product.getStates()) {
            RabinSlave.State rs = ps.secondaryStates.get(slave.mojmir.label);
            if (rs != null) { // relevant slave
                for (MojmirSlave.State fs : rs.keySet()) {
                    failP.addAll(ps, failMojmir.asMap().get(fs));
                }
            }
        }
        return failP;
    }

    // TODO: Move to MojmirSlave.State
    private TranSet<MojmirSlave.State> getFailingMojmirTransitions(RabinSlave slave, Set<MojmirSlave.State> finalStates) {
        TranSet<MojmirSlave.State> failM = new TranSet<>(valuationSetFactory);
        Collection<MojmirSlave.State> sinks = slave.mojmir.getSinks();
        sinks.removeIf(finalStates::contains);
        for (MojmirSlave.State state : slave.mojmir.getStates()) {
            for (Map.Entry<MojmirSlave.State, ValuationSet> valuationSetFailState : slave.mojmir.getSuccessors(state).entrySet()) {
                if (sinks.contains(valuationSetFailState.getKey())) {
                    failM.addAll(state, valuationSetFailState.getValue());
                }
            }
        }
        return failM;
    }

    private boolean slavesEntail(Product.ProductState ps, Map<GOperator, Integer> ranking, BitSet valuation, EquivalenceClass consequent) {
        Collection<Formula> conjunction = new ArrayList<>(3 * ranking.size());

        for (Map.Entry<GOperator, Integer> entry : ranking.entrySet()) {
            GOperator G = entry.getKey();
            int rank = entry.getValue();

            conjunction.add(G);
            if (optimisations.contains(Optimisation.EAGER)) {
                conjunction.add(G.operand);
            }

            RabinSlave.State rs = ps.secondaryStates.get(G);
            if (rs != null) {
                for (Map.Entry<MojmirSlave.State, Integer> stateEntry : rs.entrySet()) {
                    if (stateEntry.getValue() >= rank) {
                        if (optimisations.contains(Optimisation.EAGER)) {
                            conjunction.add(stateEntry.getKey().getClazz().getRepresentative().temporalStep(valuation));
                        }
                        conjunction.add(stateEntry.getKey().getClazz().getRepresentative());
                    }
                }
            }
        }

        if (optimisations.contains(Optimisation.EAGER)) {
            consequent = consequent.temporalStep(valuation);
        }

        EquivalenceClass antecedent = equivalenceClassFactory.createEquivalenceClass(new Conjunction(conjunction), formula -> {
            if (formula instanceof GOperator && !ranking.containsKey(formula)) {
                return Optional.of(Boolean.FALSE);
            }

            return Optional.empty();
        });

        return antecedent.implies(consequent);
    }

    private Collection<Map<GOperator, Integer>> powersetRanks(Deque<GOperator> gSet) {
        GOperator next = gSet.pollLast();

        if (next == null) {
            return Collections.singleton(Collections.emptyMap());
        }

        Collection<Map<GOperator, Integer>> result = new ArrayList<>();

        for (Map<GOperator, Integer> ranking : powersetRanks(gSet)) {
            for (int rank = 1; rank <= maxRank.get(next); rank++) {
                Map<GOperator, Integer> rankingNew = new HashMap<>(ranking);
                rankingNew.put(next, rank);
                result.add(rankingNew);
            }
        }

        return result;
    }

    private Formula getOverallFormula() {
        return product.primaryAutomaton.getInitialState().getClazz().getRepresentative();
    }
}
