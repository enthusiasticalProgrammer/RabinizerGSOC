package rabinizer.automata;

import com.google.common.collect.Sets;

import rabinizer.ltl.equivalence.EquivalenceClass;
import rabinizer.ltl.equivalence.EquivalenceClassFactory;
import rabinizer.exec.Main;
import rabinizer.ltl.*;
import rabinizer.ltl.simplifier.Simplifier;
import rabinizer.collections.valuationset.ValuationSet;
import rabinizer.collections.valuationset.ValuationSetFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/*
 * @author jkretinsky
 * */
public class AccLocal {

    protected final ValuationSetFactory valuationSetFactory;
    protected final EquivalenceClassFactory equivalenceClassFactory;

    protected final Product product;
    protected final Formula formula;
    protected final Map<Formula, Integer> maxRank = new HashMap<>();
    protected final Map<Formula, Set<GOperator>> topmostGs = new HashMap<>();
    protected final TranSet<Product.ProductState> allTrans;
    private final boolean gSkeleton;

    // separate automata acceptance projected to the whole product
    Map<Formula, Map<Set<GOperator>, Map<Integer, RabinPair<Product.ProductState>>>> accSlavesOptions = new HashMap<>();
    Map<Set<GOperator>, Map<Map<Formula, Integer>, RabinPair<Product.ProductState>>> accMasterOptions = new HashMap<>();
    // actually just coBuchi

    public AccLocal(Product product, ValuationSetFactory factory, EquivalenceClassFactory factory2, Collection<Optimisation> opts) {
        this.product = product;
        // TODO: Drop this.formula
        this.formula = product.primaryAutomaton.getInitialState().getClazz().getRepresentative();
        this.valuationSetFactory = factory;
        this.equivalenceClassFactory = factory2;
        allTrans = new TranSet<>(valuationSetFactory);
        gSkeleton = opts.contains(Optimisation.SKELETON);
        for (GOperator f : formula.gSubformulas()) {
            int maxRankF = 0;
            for (RabinSlave.State rs : product.secondaryAutomata.get(f).states) {
                maxRankF = maxRankF >= rs.size() ? maxRankF : rs.size();
            }
            maxRank.put(f, maxRankF);
            topmostGs.put(f, new HashSet<>(f.operand.topmostGs()));
            Map<Set<GOperator>, Map<Integer, RabinPair<Product.ProductState>>> optionForf = computeAccSlavesOptions(f, false);
            accSlavesOptions.put(f, optionForf);
        }
        Main.verboseln("Acceptance for secondaryAutomata:\n" + this.accSlavesOptions);
        ValuationSet allVals = valuationSetFactory.createUniverseValuationSet();
        for (Product.ProductState ps : product.getStates()) {
            allTrans.add(ps, allVals);
        }

        accMasterOptions = computeAccMasterOptions();
        Main.verboseln("Acceptance for primaryAutomaton:\n" + this.accMasterOptions);
    }

    protected boolean slavesEntail(Set<GOperator> gSet, Product.ProductState ps, Map<Formula, Integer> ranking, Set<String> v,
                                   EquivalenceClass consequent) {
        Formula antecedent = BooleanConstant.get(true);
        for (GOperator f : gSet) {
            antecedent = Simplifier.simplify(new Conjunction(antecedent, f), Simplifier.Strategy.PROPOSITIONAL); // TODO compute
            // these lines
            // once for all
            // states
            antecedent = Simplifier.simplify(new Conjunction(antecedent, f.operand.evaluate(gSet)), Simplifier.Strategy.PROPOSITIONAL);

            Formula slaveAntecedent = BooleanConstant.get(true);

            if (ps.getSecondaryMap().containsKey(f)) {
                for (MojmirSlave.State s : ps.getSecondaryState(f).keySet()) {
                    if (ps.getSecondaryState(f).get(s) >= ranking.get(f)) {
                        slaveAntecedent = Simplifier.simplify(new Conjunction(slaveAntecedent, s.getClazz().getRepresentative()), Simplifier.Strategy.PROPOSITIONAL);
                    }
                }
            }

            slaveAntecedent = slaveAntecedent.temporalStep(v).evaluate(gSet);
            antecedent = Simplifier.simplify(new Conjunction(antecedent, slaveAntecedent), Simplifier.Strategy.PROPOSITIONAL);
        }

        EquivalenceClass antClazz = equivalenceClassFactory.createEquivalenceClass(antecedent);
        return antClazz.implies(consequent.temporalStep(v));
    }

    protected Map<Set<GOperator>, Map<Integer, RabinPair<Product.ProductState>>> computeAccSlavesOptions(GOperator g, boolean forceAllSlaves) {
        Map<Set<GOperator>, Map<Integer, RabinPair<Product.ProductState>>> result = new HashMap<>();
        RabinSlave rSlave = product.secondaryAutomata.get(g);
        Set<Set<GOperator>> gSets;
        if (gSkeleton || forceAllSlaves) {
            gSets = g.operand.accept(new SkeletonVisitor());
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

    protected Map<Set<GOperator>, Map<Map<Formula, Integer>, RabinPair<Product.ProductState>>> computeAccMasterOptions() {
        Map<Set<GOperator>, Map<Map<Formula, Integer>, RabinPair<Product.ProductState>>> result = new HashMap<>();

        Set<Set<GOperator>> gSets;
        if (gSkeleton) {
            gSets = formula.accept(new SkeletonVisitor());
        } else {
            gSets = Sets.powerSet(formula.gSubformulas());
        }
        for (Set<GOperator> gSet : gSets) {
            Main.verboseln("\tGSet " + gSet);

            Set<Map<Formula, Integer>> rankings = powersetRanks(new HashSet<>(gSet));
            for (Map<Formula, Integer> ranking : rankings) {
                Main.verboseln("\t  Ranking " + ranking);
                TranSet<Product.ProductState> avoidP = new TranSet<>(valuationSetFactory);
                for (Product.ProductState ps : product.states) {
                    avoidP.addAll(computeAccMasterForState(gSet, ranking, ps));
                }
                if (avoidP.equals(allTrans)) {
                    Main.verboseln("Skipping complete Avoid");
                } else {
                    if (!result.containsKey(gSet)) {
                        result.put(gSet, new HashMap<>());
                    }
                    if (!result.get(gSet).containsKey(ranking)) {
                        result.get(gSet).put(ranking, new RabinPair(avoidP, null));
                    }
                    Main.verboseln("Avoid for " + gSet + ranking + "\n" + avoidP);
                }
            }
        }

        return result;
    }

    protected Set<Map<Formula, Integer>> powersetRanks(Set<GOperator> gSet) {
        Set<Map<Formula, Integer>> result = new HashSet<>();
        if (gSet.isEmpty()) {
            result.add(new HashMap<>());
        } else {
            Formula curr = gSet.iterator().next();
            gSet.remove(curr);
            for (Map<Formula, Integer> ranking : powersetRanks(gSet)) {
                for (int rank = 1; rank <= maxRank.get(curr); rank++) {
                    Map<Formula, Integer> rankingNew = new HashMap<>(ranking);
                    rankingNew.put(curr, rank);
                    result.add(rankingNew);
                }
            }
        }
        return result;
    }

    // symbolic version
    protected TranSet<Product.ProductState> computeAccMasterForState(Set<GOperator> gSet, Map<Formula, Integer> ranking, Product.ProductState ps) {
        TranSet<Product.ProductState> result = new TranSet<>(valuationSetFactory);
        Set<ValuationSet> fineSuccVs = product.generateSuccTransitionsReflectingSinks(ps);
        for (ValuationSet vs : fineSuccVs) {
            if (!slavesEntail(gSet, ps, ranking, vs.pickAny(), ps.getPrimaryState().getClazz())) {
                result.add(ps, vs);
            }
        }
        return result;
    }
}
