package rabinizer.automata;

import com.google.common.collect.Sets;
import rabinizer.exec.Main;
import rabinizer.ltl.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/*
 * @author jkretinsky
 * */
public class AccLocal {

    protected final ValuationSetFactory<String> valuationSetFactory;
    protected final EquivalenceClassFactory equivalenceClassFactory;

    protected final Product product;
    protected final Formula formula;
    protected final Map<Formula, Integer> maxRank = new HashMap<>();
    final Map<Formula, Set<GOperator>> topmostGs = new HashMap<>();
    final TranSet<Product.ProductState> allTrans;
    // separate automata acceptance projected to the whole product
    Map<Formula, Map<Set<GOperator>, Map<Integer, RabinPair>>> accSlavesOptions = new HashMap<>();
    Map<Set<GOperator>, Map<Map<Formula, Integer>, RabinPair>> accMasterOptions = new HashMap<>();
    // actually just coBuchi

    public AccLocal(Product product, ValuationSetFactory<String> factory, EquivalenceClassFactory factory2) {
        this.product = product;
        // TODO: Drop this.formula
        this.formula = product.primaryAutomaton.getInitialState().getClazz().getRepresentative();
        this.valuationSetFactory = factory;
        this.equivalenceClassFactory = factory2;
        allTrans = new TranSet<>(valuationSetFactory);
        for (GOperator f : formula.gSubformulas()) {
            int maxRankF = 0;
            for (RabinSlave.State rs : product.secondaryAutomata.get(f).states) {
                maxRankF = maxRankF >= rs.size() ? maxRankF : rs.size();
            }
            maxRank.put(f, maxRankF);
            topmostGs.put(f, new HashSet<>(f.topmostGs()));
            Map<Set<GOperator>, Map<Integer, RabinPair>> optionForf = computeAccSlavesOptions(f);
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

    // checks if antecedent =>consequent
    // Formula:
    public boolean entails(Formula antecedent, EquivalenceClass consequent) {
        EquivalenceClass antClazz = equivalenceClassFactory.createEquivalenceClass(antecedent);
        return antClazz.implies(consequent);
    }

    protected boolean slavesEntail(Set<GOperator> gSet, Product.ProductState ps, Map<Formula, Integer> ranking, Set<String> v,
                                   EquivalenceClass consequent) {
        Formula antecedent = BooleanConstant.get(true);
        for (GOperator f : gSet) {
            antecedent = FormulaFactory.mkAnd(antecedent, f); // TODO compute
            // these lines
            // once for all
            // states
            antecedent = FormulaFactory.mkAnd(antecedent, f.operand.evaluate(gSet));

            Formula slaveAntecedent = BooleanConstant.get(true);

            if (ps.getSecondaryMap().containsKey(f)) {
                for (MojmirSlave.State s : (ps.getSecondaryState(f)).keySet()) {
                    if ((ps.getSecondaryState(f)).get(s) >= ranking.get(f)) {
                        slaveAntecedent = FormulaFactory.mkAnd(slaveAntecedent, s.getClazz().getRepresentative());
                    }
                }
            }

            slaveAntecedent = slaveAntecedent.temporalStep(v).evaluate(gSet);
            antecedent = FormulaFactory.mkAnd(antecedent, slaveAntecedent);
        }

        return entails(antecedent, consequent.temporalStep(v));
    }

    protected Map<Set<GOperator>, Map<Integer, RabinPair>> computeAccSlavesOptions(Formula f) {
        Map<Set<GOperator>, Map<Integer, RabinPair>> result = new HashMap<>();
        RabinSlave rSlave = (RabinSlave) product.secondaryAutomata.get(f);
        Set<Set<GOperator>> gSets = Sets.powerSet(topmostGs.get(f));
        for (Set<GOperator> gSet : gSets) {
            Set<IState> finalStates = new HashSet<>();

            for (MojmirSlave.State fs : rSlave.mojmir.states) {
                if (equivalenceClassFactory.createEquivalenceClass(new Conjunction(gSet)).implies((fs).getClazz())) {
                    finalStates.add(fs);
                }
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
                TranSet avoidP = new TranSet(valuationSetFactory);
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
    protected TranSet computeAccMasterForState(Set<GOperator> gSet, Map<Formula, Integer> ranking, Product.ProductState ps) {
        TranSet result = new TranSet(valuationSetFactory);
        Set<ValuationSet> fineSuccVs = product.generateSuccTransitionsReflectingSinks(ps);
        for (ValuationSet vs : fineSuccVs) {
            if (!slavesEntail(gSet, ps, ranking, vs.pickAny(), ps.getPrimaryState().getClazz())) {
                result.add(ps, vs);
            }
        }
        return result;
    }

    // unused: explicit version (simpler, likely slower)
    protected TranSet computeAccMasterForState2(Set<GOperator> gSet, Map<Formula, Integer> ranking, Product.ProductState ps) {
        TranSet result = new TranSet(valuationSetFactory);
        for (Set<String> v : valuationSetFactory.createUniverseValuationSet()) { // TODO
            // !!!
            // expl
            // vs
            // bdd
            if (!slavesEntail(gSet, ps, ranking, v, ps.getPrimaryState().getClazz())) {
                result.add(ps, valuationSetFactory.createValuationSet(v));
            }
        }
        return result;
    }
}
