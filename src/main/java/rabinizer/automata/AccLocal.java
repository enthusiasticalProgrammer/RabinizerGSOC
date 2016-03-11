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

public class AccLocal {

    protected final ValuationSetFactory valuationSetFactory;
    protected final EquivalenceClassFactory equivalenceClassFactory;

    protected final Product product;
    protected final Formula formula;
    protected final Map<GOperator, Integer> maxRank = new HashMap<>();
    protected final Map<GOperator, Set<GOperator>> topmostGs = new HashMap<>();
    protected final TranSet<Product.ProductState> allTrans;
    // separate automata acceptance projected to the whole product
    final Map<GOperator, Map<Set<GOperator>, Map<Integer, RabinPair<Product.ProductState>>>> accSlavesOptions = new HashMap<>();
    // actually just coBuchi
    final Map<Map<GOperator, Integer>, RabinPair<Product.ProductState>> accMasterOptions;
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
            for (RabinSlave.State rs : product.secondaryAutomata.get(f).states) {
                maxRankF = maxRankF >= rs.size() ? maxRankF : rs.size();
            }
            maxRank.put(f, maxRankF);
            topmostGs.put(f, f.operand.topmostGs());
            Map<Set<GOperator>, Map<Integer, RabinPair<Product.ProductState>>> optionForf = computeAccSlavesOptions(f, false);
            accSlavesOptions.put(f, optionForf);
        }

        Main.verboseln("Acceptance for secondaryAutomata:\n" + this.accSlavesOptions);
        ValuationSet allVals = valuationSetFactory.createUniverseValuationSet();
        for (Product.ProductState ps : product.getStates()) {
            allTrans.addAll(ps, allVals);
        }

        accMasterOptions = computeAccMasterOptions();
        Main.verboseln("Acceptance for primaryAutomaton:\n" + this.accMasterOptions);
    }

    protected boolean slavesEntail(Product.ProductState ps, Map<GOperator, Integer> ranking, Set<String> v, EquivalenceClass consequent) {
        Set<GOperator> gSet = ranking.keySet();

        Collection<Formula> conjunction = new ArrayList<>(3 * gSet.size());

        if (eager) {
            for (GOperator f : gSet) {
                conjunction.add(f);
                conjunction.add(f.operand.evaluate(gSet));
                RabinSlave.State rs = ps.getSecondaryState(f);
                if (rs != null) {
                    for (Map.Entry<MojmirSlave.State, Integer> entry : rs.entrySet()) {
                        if (entry.getValue() >= ranking.get(f)) {
                            conjunction.add(entry.getKey().getClazz().getRepresentative().temporalStep(v).evaluate(gSet));
                        }
                    }
                }
            }

            consequent = consequent.temporalStep(v);
        } else {
            for (GOperator f : gSet) {
                conjunction.add(f);
                RabinSlave.State rs = ps.getSecondaryState(f);
                if (rs != null) {
                    for (Map.Entry<MojmirSlave.State, Integer> entry : rs.entrySet()) {
                        if (entry.getValue() >= ranking.get(f)) {
                            conjunction.add(entry.getKey().getClazz().getRepresentative().evaluate(gSet));
                        }
                    }
                }
            }
        }

        EquivalenceClass antecedent = equivalenceClassFactory.createEquivalenceClass(new Conjunction(conjunction));
        return antecedent.implies(consequent);
    }

    protected Map<Set<GOperator>, Map<Integer, RabinPair<Product.ProductState>>> computeAccSlavesOptions(GOperator g, boolean forceAllSlaves) {
        Map<Set<GOperator>, Map<Integer, RabinPair<Product.ProductState>>> result = new HashMap<>();

        RabinSlave rSlave = product.secondaryAutomata.get(g);
        Set<Set<GOperator>> gSets;
        if (gSkeleton && !forceAllSlaves) {
            gSets = g.operand.accept(new SkeletonVisitor(SkeletonVisitor.SkeletonApproximation.LOWER_BOUND));
            gSets.retainAll(Sets.powerSet(topmostGs.get(g)));
        } else {
            gSets = Sets.powerSet(topmostGs.get(g));
        }

        for (Set<GOperator> gSet : gSets) {
            Set<MojmirSlave.State> finalStates = new HashSet<>();
            EquivalenceClass gSetClazz = equivalenceClassFactory.createEquivalenceClass(new Conjunction(gSet));

            for (MojmirSlave.State fs : rSlave.mojmir.states) {
                if (gSetClazz.implies(fs.getClazz())) {
                    finalStates.add(fs);
                }
            }

            result.put(gSet, new HashMap<>());
            for (int rank = 1; rank <= maxRank.get(g); rank++) {
                result.get(gSet).put(rank, RabinPair.createRabinPair(rSlave, finalStates, rank, product, valuationSetFactory));
            }
        }

        return result;

    }

    protected Map<Map<GOperator, Integer>, RabinPair<Product.ProductState>> computeAccMasterOptions() {
        ImmutableMap.Builder<Map<GOperator, Integer>, RabinPair<Product.ProductState>> builder = ImmutableMap.builder();

        Set<Set<GOperator>> gSets;
        if (gSkeleton) {
            gSets = formula.accept(new SkeletonVisitor(SkeletonVisitor.SkeletonApproximation.LOWER_BOUND));
        } else {
            gSets = Sets.powerSet(formula.gSubformulas());
        }

        for (Set<GOperator> gSet : gSets) {
            Main.verboseln("\tGSet " + gSet);

            for (Map<GOperator, Integer> ranking : powersetRanks(new HashSet<>(gSet))) {
                Main.verboseln("\t  Ranking " + ranking);

                TranSet<Product.ProductState> avoidP = new TranSet<>(valuationSetFactory);

                for (Product.ProductState ps : product.states) {
                    avoidP.addAll(computeAccMasterForState(ranking, ps));
                }

                if (avoidP.equals(allTrans)) {
                    Main.verboseln("Skipping complete Avoid");
                    continue;
                }

                builder.put(ImmutableMap.copyOf(ranking), new RabinPair<>(avoidP, null));
                Main.verboseln("Avoid for " + gSet + ranking + "\n" + avoidP);
            }
        }

        return builder.build();
    }


    protected Collection<Map<GOperator, Integer>> powersetRanks(Collection<GOperator> gSet) {
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

    protected TranSet<Product.ProductState> computeAccMasterForState(Map<GOperator, Integer> ranking, Product.ProductState ps) {
        TranSet<Product.ProductState> result = new TranSet<>(valuationSetFactory);

        if (eager) {
            Set<ValuationSet> fineSuccVs = product.generateSuccTransitionsReflectingSinks(ps);
            for (ValuationSet vs : fineSuccVs) {
                if (!slavesEntail(ps, ranking, vs.pickAny(), ps.getPrimaryState().getClazz())) {
                    result.addAll(ps, vs);
                }
            }
        } else {
            if (!slavesEntail(ps, ranking, null, ps.getPrimaryState().getClazz())) {
                result.addAll(ps, valuationSetFactory.createUniverseValuationSet());
            }
        }

        return result;
    }
}
