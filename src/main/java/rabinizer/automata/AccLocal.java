/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rabinizer.automata;


import com.google.common.collect.Sets;
import rabinizer.exec.Main;
import rabinizer.ltl.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * @author jkretinsky & Christopher Ziegler
 */
public class AccLocal {

    protected final ValuationSetFactory<String> valuationSetFactory;
    protected final EquivalenceClassFactory equivalenceClassFactory;

    protected final Product product;
    protected final Formula formula;
    protected final Map<Formula, Integer> maxRank = new HashMap<>();
    final Map<Formula, Set<GOperator>> topmostGs = new HashMap<>(); // without the outer G
    final TranSet<ProductState> allTrans;
    // separate automata acceptance projected to the whole product
    Map<Formula, Map<Set<GOperator>, Map<Integer, RabinPair>>> accSlavesOptions = new HashMap<>();
    Map<Set<GOperator>, Map<Map<Formula, Integer>, RabinPair>> accMasterOptions = new HashMap<>();
    // actually just coBuchi

    public AccLocal(Product product, ValuationSetFactory<String> factory, EquivalenceClassFactory factory2) {
        this.product = product;
        this.formula = product.master.formula;
        this.valuationSetFactory = factory;
        this.equivalenceClassFactory = factory2;
        allTrans = new TranSet<>(valuationSetFactory);
        for (Formula f : formula.gSubformulas()) {
            int maxRankF = 0;
            for (RankingState rs : product.slaves.get(f).states) {
                maxRankF = maxRankF >= rs.size() ? maxRankF : rs.size();
            }
            maxRank.put(f, maxRankF);
            topmostGs.put(f, new HashSet<>(f.topmostGs()));
            Map<Set<GOperator>, Map<Integer, RabinPair>> optionForf = computeAccSlavesOptions(f);
            accSlavesOptions.put(f, optionForf);
        }
        Main.verboseln("Acceptance for slaves:\n" + this.accSlavesOptions);
        ValuationSet allVals = valuationSetFactory.createUniverseValuationSet();
        for (ProductState ps : product.states) {
            allTrans.add(ps, allVals);
        }

        accMasterOptions = computeAccMasterOptions();
        Main.verboseln("Acceptance for master:\n" + this.accMasterOptions);
    }

    protected boolean slavesEntail(Set<GOperator> gSet, ProductState ps, Map<Formula, Integer> ranking, Set<String> v, Formula consequent) {
        Formula antecedent = BooleanConstant.get(true);
        for (GOperator f : gSet) {
            antecedent = FormulaFactory.mkAnd(antecedent, FormulaFactory.mkG(f)); // TODO compute these lines once for all states
            antecedent = FormulaFactory.mkAnd(antecedent, f.evaluate(gSet));
            Formula slaveAntecedent = BooleanConstant.get(true);
            if (ps.containsKey(f)) {
                for (FormulaState s : ps.get(f).keySet()) {
                    if (ps.get(f).get(s) >= ranking.get(f)) {
                        slaveAntecedent = FormulaFactory.mkAnd(slaveAntecedent, s.getFormula());
                    }
                }
            }
            slaveAntecedent = slaveAntecedent.temporalStep(v).evaluate(gSet);
            antecedent = FormulaFactory.mkAnd(antecedent, slaveAntecedent);
        }
        return entails(antecedent, consequent.temporalStep(v));
    }

    //checks if antecedent =>consequent
    //Formula:
    public boolean entails(Formula antecedent, Formula consequent) {
        EquivalenceClass antClazz = equivalenceClassFactory.createEquivalenceClass(antecedent);
        EquivalenceClass conClazz = equivalenceClassFactory.createEquivalenceClass(consequent);
        return antClazz.implies(conClazz);
    }

    protected Map<Set<GOperator>, Map<Integer, RabinPair>> computeAccSlavesOptions(Formula f) {
        Map<Set<GOperator>, Map<Integer, RabinPair>> result = new HashMap<>();
        RabinSlave rSlave = product.slaves.get(f);
        Set<Set<GOperator>> gSets = Sets.powerSet(topmostGs.get(f));
        for (Set<GOperator> gSet : gSets) {
            Map<FormulaState, Boolean> finalStates = new HashMap<>();
            for (FormulaState fs : rSlave.mojmir.states) {
                finalStates.put(fs, entails(new Conjunction(gSet),fs.getFormula()));
            }
            result.put(gSet, new HashMap<>());
            for (int rank = 1; rank <= maxRank.get(f); rank++) {
                result.get(gSet).put(rank, new RabinPair(rSlave, finalStates, rank, product, valuationSetFactory));
            }
        }

        return result;
    }

    protected Map<Set<GOperator>, Map<Map<Formula, Integer>, RabinPair>> computeAccMasterOptions() {
        Map<Set<GOperator>, Map<Map<Formula, Integer>, RabinPair>> result = new HashMap<>();

        Set<Set<GOperator>> gSets = Sets.powerSet(formula.gSubformulas());
        for (Set<GOperator> gSet : gSets) {
            Main.verboseln("\tGSet " + gSet);

            Set<Map<Formula, Integer>> rankings = powersetRanks(new HashSet<>(gSet));
            for (Map<Formula, Integer> ranking : rankings) {
                Main.verboseln("\t  Ranking " + ranking);
                TranSet<ProductState> avoidP = new TranSet<>(valuationSetFactory);
                for (ProductState ps : product.states) {
                    /*
                     for (Valuation v : AllValuations.allValuations) { // TODO !!! expl vs bdd
                     if (!slavesEntail(gSet, gSetComplement, ps, ranking, v, ps.masterState.formula)) {
                     avoidP.add(ps, new ValuationSetExplicit(v));
                     }
                     }
                     */
                    avoidP.addAll(computeAccMasterForState(gSet, ranking, ps));
                }
                if (avoidP.equals(allTrans)) {
                    Main.verboseln("Skipping complete Avoid");
                } else {
                    if (!result.containsKey(gSet)) {
                        result.put(gSet, new HashMap<>());
                    }
                    if (!result.get(gSet).containsKey(ranking)) {
                        result.get(gSet).put(ranking, new RabinPair<>(avoidP, null));
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
    protected TranSet<ProductState> computeAccMasterForState(Set<GOperator> gSet, Map<Formula, Integer> ranking, ProductState ps) {
        TranSet<ProductState> result = new TranSet<>(valuationSetFactory);
        Set<ValuationSet> fineSuccVs = product.generateSuccTransitionsReflectingSinks(ps);
        for (ValuationSet vs : fineSuccVs) {
            if (!slavesEntail(gSet, ps, ranking, vs.pickAny(), ps.masterState.getFormula())) {
                result.add(ps, vs);
            }
        }
        return result;
    }

    // unused: explicit version (simpler, likely slower)
    protected TranSet<ProductState> computeAccMasterForState2(Set<GOperator> gSet, Map<Formula, Integer> ranking, ProductState ps) {
        TranSet<ProductState> result = new TranSet<>(valuationSetFactory);
        for (Set<String> v : valuationSetFactory.createUniverseValuationSet()) { // TODO !!! expl vs bdd
            if (!slavesEntail(gSet, ps, ranking, v, ps.masterState.getFormula())) {
                result.add(ps, valuationSetFactory.createValuationSet(v));
            }
        }
        return result;
    }
}
