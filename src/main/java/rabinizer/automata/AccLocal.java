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
import rabinizer.exec.Main;
import rabinizer.ltl.Conjunction;
import rabinizer.ltl.Formula;
import rabinizer.ltl.GOperator;
import rabinizer.ltl.SkeletonVisitor;
import rabinizer.ltl.equivalence.EquivalenceClass;
import rabinizer.ltl.equivalence.EquivalenceClassFactory;

import java.util.*;

class AccLocal {

    final Map<GOperator, Set<GOperator>> topmostGs = new HashMap<>();
    final TranSet<Product.ProductState> allTrans;
    // separate automata acceptance projected to the whole product
    final Map<GOperator, Map<Set<GOperator>, Map<Integer, RabinPair<Product.ProductState>>>> accSlavesOptions = new HashMap<>();
    // actually just coBuchi
    final Map<Map<GOperator, Integer>, TranSet<Product.ProductState>> accMasterOptions;
    private final ValuationSetFactory valuationSetFactory;
    private final EquivalenceClassFactory equivalenceClassFactory;
    private final Product product;
    private final Formula formula;
    private final Map<GOperator, Integer> maxRank = new HashMap<>();
    private final boolean gSkeleton;
    private final boolean eager;

    public AccLocal(Product product, ValuationSetFactory factory, EquivalenceClassFactory factory2, Collection<Optimisation> opts) {
        this.product = product;
        // TODO: Drop this.formula
        this.formula = product.primaryAutomaton.getInitialState().getClazz().getRepresentative();
        this.valuationSetFactory = factory;
        this.equivalenceClassFactory = factory2;
        allTrans = new TranSet<>(valuationSetFactory);
        gSkeleton = opts.contains(Optimisation.SKELETON);
        eager = opts.contains(Optimisation.EAGER);

        for (GOperator f : formula.gSubformulas()) {
            int maxRankF = 0;
            for (RabinSlave.State rs : product.secondaryAutomata.get(f).getStates()) {
                maxRankF = maxRankF >= rs.size() ? maxRankF : rs.size();
            }
            maxRank.put(f, maxRankF);
            topmostGs.put(f, f.operand.topmostGs());
            Map<Set<GOperator>, Map<Integer, RabinPair<Product.ProductState>>> optionForf = computeAccSlavesOptions(f, false);
            accSlavesOptions.put(f, optionForf);
        }

        ValuationSet allVals = valuationSetFactory.createUniverseValuationSet();
        for (Product.ProductState ps : product.getStates()) {
            allTrans.addAll(ps, allVals);
        }

        accMasterOptions = computeAccMasterOptions();
    }

    private static RabinPair<Product.ProductState> createRabinPair(RabinSlave slave, Set<MojmirSlave.State> finalStates, int rank, Product product,
                                                                   ValuationSetFactory valuationSetFactory) {
        // Set fail
        // Mojmir
        TranSet<MojmirSlave.State> failM = new TranSet<>(valuationSetFactory);
        for (MojmirSlave.State fs : slave.mojmir.getStates()) {
            for (Map.Entry<MojmirSlave.State, ValuationSet> vsfs : slave.mojmir.getSuccessors(fs).entrySet()) {
                if (slave.mojmir.isSink(vsfs.getKey()) && !finalStates.contains(vsfs.getKey())) {
                    failM.addAll(fs, vsfs.getValue());
                }
            }
        }

        // Product
        TranSet<Product.ProductState> failP = new TranSet<>(valuationSetFactory);
        for (Product.ProductState ps : product.getStates()) {
            RabinSlave.State rs = ps.secondaryStates.get(slave.mojmir.label);
            if (rs != null) { // relevant slave
                for (MojmirSlave.State fs : rs.keySet()) {
                    failP.addAll(ps, failM.asMap().get(fs));
                }
            }
        }

        // Set succeed(pi)
        // Mojmir
        TranSet<MojmirSlave.State> succeedM = new TranSet<>(valuationSetFactory);
        if (finalStates.contains(slave.mojmir.getInitialState())) {
            for (MojmirSlave.State fs : slave.mojmir.getStates()) {
                for (Map.Entry<MojmirSlave.State, ValuationSet> vsfs : slave.mojmir.getSuccessors(fs).entrySet()) {
                    succeedM.addAll(fs, vsfs.getValue());
                }
            }
        } else {
            for (MojmirSlave.State fs : slave.mojmir.getStates()) {
                if (!finalStates.contains(fs)) {
                    for (Map.Entry<MojmirSlave.State, ValuationSet> vsfs : slave.mojmir.getSuccessors(fs).entrySet()) {
                        if (finalStates.contains(vsfs.getKey())) {
                            succeedM.addAll(fs, vsfs.getValue());
                        }
                    }
                }
            }
        }
        // Product
        TranSet<Product.ProductState> succeedP = new TranSet<>(valuationSetFactory);
        for (Product.ProductState ps : product.getStates()) {
            RabinSlave.State rs = ps.secondaryStates.get(slave.mojmir.label);
            if (rs != null) { // relevant slave
                for (Map.Entry<MojmirSlave.State, Integer> stateIntegerEntry : rs.entrySet()) {
                    if (stateIntegerEntry.getValue() != rank) {
                        continue;
                    }

                    succeedP.addAll(ps, succeedM.asMap().get(stateIntegerEntry.getKey()));
                }
            }
        }

        // Set buy(pi)
        // Rabin
        TranSet<RabinSlave.State> buyR = new TranSet<>(valuationSetFactory);
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
                                    buyR.addAll(rs, vs1copy);
                                } else if (succ.equals(slave.mojmir.getInitialState())) {
                                    buyR.addAll(rs, vs1);
                                }

                            }
                        }
                    }
                }
            }
        }
        // Product
        TranSet<Product.ProductState> buyP = new TranSet<>(valuationSetFactory);
        for (Product.ProductState ps : product.getStates()) {
            RabinSlave.State rs = ps.secondaryStates.get(slave.mojmir.label);
            if (rs != null) { // relevant slave
                buyP.addAll(ps, buyR.asMap().get(rs));
            }
        }

        failP.addAll(buyP);
        return new RabinPair<>(failP, succeedP);
    }

    Map<Set<GOperator>, Map<Integer, RabinPair<Product.ProductState>>> computeAccSlavesOptions(GOperator g, boolean forceAllSlaves) {
        Map<Set<GOperator>, Map<Integer, RabinPair<Product.ProductState>>> result = new HashMap<>();

        RabinSlave rSlave = product.secondaryAutomata.get(g);
        Set<Set<GOperator>> gSets;
        if (gSkeleton && !forceAllSlaves) {
            gSets = g.operand.accept(SkeletonVisitor.getInstance(SkeletonVisitor.SkeletonApproximation.LOWER_BOUND));
            gSets.retainAll(Sets.powerSet(topmostGs.get(g)));
        } else {
            gSets = Sets.powerSet(topmostGs.get(g));
        }

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
                result.get(gSet).put(rank, createRabinPair(rSlave, finalStates, rank, product, valuationSetFactory));
            }
        }

        return result;
    }

    private boolean slavesEntail(Product.ProductState ps, Map<GOperator, Integer> ranking, BitSet v, EquivalenceClass consequent) {
        Set<GOperator> gSet = ranking.keySet();

        Collection<Formula> conjunction = new ArrayList<>(3 * gSet.size());

        if (eager) {
            for (Map.Entry<GOperator, Integer> entry : ranking.entrySet()) {
                GOperator G = entry.getKey();
                int rank = entry.getValue();

                conjunction.add(G);
                conjunction.add(G.operand);
                RabinSlave.State rs = ps.secondaryStates.get(G);
                if (rs != null) {
                    for (Map.Entry<MojmirSlave.State, Integer> stateEntry : rs.entrySet()) {
                        if (stateEntry.getValue() >= rank) {
                            conjunction.add(stateEntry.getKey().getClazz().getRepresentative().temporalStep(v));
                        }
                    }
                }
            }

            consequent = consequent.temporalStep(v);
        } else {
            for (Map.Entry<GOperator, Integer> entry : ranking.entrySet()) {
                GOperator G = entry.getKey();
                int rank = entry.getValue();

                conjunction.add(G);
                RabinSlave.State rs = ps.secondaryStates.get(G);
                if (rs != null) {
                    for (Map.Entry<MojmirSlave.State, Integer> stateEntry : rs.entrySet()) {
                        if (stateEntry.getValue() >= rank) {
                            conjunction.add(stateEntry.getKey().getClazz().getRepresentative());
                        }
                    }
                }
            }
        }

        EquivalenceClass antecedent = equivalenceClassFactory.createEquivalenceClass(new Conjunction(conjunction), formula -> {
            if (formula instanceof GOperator && !ranking.containsKey(formula)) {
                return Optional.of(Boolean.FALSE);
            }

            return Optional.empty();
        });

        return antecedent.implies(consequent);
    }

    private Map<Map<GOperator, Integer>, TranSet<Product.ProductState>> computeAccMasterOptions() {
        ImmutableMap.Builder<Map<GOperator, Integer>, TranSet<Product.ProductState>> builder = ImmutableMap.builder();

        Set<Set<GOperator>> gSets;
        if (gSkeleton) {
            gSets = formula.accept(SkeletonVisitor.getInstance(SkeletonVisitor.SkeletonApproximation.LOWER_BOUND));
        } else {
            gSets = Sets.powerSet(formula.gSubformulas());
        }

        for (Set<GOperator> gSet : gSets) {

            for (Map<GOperator, Integer> ranking : powersetRanks(new HashSet<>(gSet))) {

                TranSet<Product.ProductState> avoidP = new TranSet<>(valuationSetFactory);

                for (Product.ProductState ps : product.getStates()) {
                    avoidP.addAll(computeAccMasterForState(ranking, ps));
                }

                if (avoidP.equals(allTrans)) {
                    continue;
                }

                builder.put(ImmutableMap.copyOf(ranking), avoidP);
            }
        }

        return builder.build();
    }


    private Collection<Map<GOperator, Integer>> powersetRanks(Collection<GOperator> gSet) {
        if (gSet.isEmpty()) {
            return Collections.singleton(Collections.emptyMap());
        }

        Collection<Map<GOperator, Integer>> result = new ArrayList<>();

        GOperator curr = gSet.iterator().next();
        gSet.remove(curr);

        for (Map<GOperator, Integer> ranking : powersetRanks(gSet)) {
            for (int rank = 1; rank <= maxRank.get(curr); rank++) {
                Map<GOperator, Integer> rankingNew = new HashMap<>(ranking);
                rankingNew.put(curr, rank);
                result.add(rankingNew);
            }
        }

        return result;
    }

    private TranSet<Product.ProductState> computeAccMasterForState(Map<GOperator, Integer> ranking, Product.ProductState ps) {
        TranSet<Product.ProductState> result = new TranSet<>(valuationSetFactory);

        if (eager) {
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
}
