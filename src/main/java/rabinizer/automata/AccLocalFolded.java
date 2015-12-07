/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rabinizer.automata;

import rabinizer.ltl.*;

import java.util.Map;
import java.util.Set;

/**
 * @author jkretinsky
 */
public class AccLocalFolded extends AccLocal {

    public AccLocalFolded(Product product, ValuationSetFactory<String> factory, EquivalenceClassFactory factory2) {
        super(product, factory, factory2);
    }

    protected boolean slavesEntail(GSet gSet, GSet gSetComplement, ProductState ps, Map<Formula, Integer> ranking, Set<String> v, Formula consequent) {
        Formula antecedent = BooleanConstant.get(true);
        for (Formula f : gSet) {
            antecedent = FormulaFactory.mkAnd(antecedent, FormulaFactory.mkG(f)); // TODO relevant for Folded version
            //antecedent = new Conjunction(antecedent, new XOperator(new GOperator(f))); // TODO:remove; relevant for Xunfolding
            Formula slaveAntecedent = BooleanConstant.get(true);
            if (ps.containsKey(f)) {
                for (FormulaState s : ps.get(f).keySet()) {
                    if (ps.get(f).get(s) >= ranking.get(f)) {
                        slaveAntecedent = FormulaFactory.mkAnd(slaveAntecedent, s.getFormula());
                    }
                }
            }
            slaveAntecedent = slaveAntecedent.substituteGsToFalse(gSetComplement);
            antecedent = FormulaFactory.mkAnd(antecedent, slaveAntecedent);
        }
        return entails(antecedent, consequent);
    }

    @Override
    protected TranSet<ProductState> computeAccMasterForState(GSet gSet, GSet gSetComplement, Map<Formula, Integer> ranking, ProductState ps) {
        TranSet<ProductState> result = new TranSet<>(valuationSetFactory);
        if (!slavesEntail(gSet, gSetComplement, ps, ranking, null, ps.masterState.getFormula())) {
            result.add(ps, valuationSetFactory.createUniverseValuationSet());
        }
        return result;
    }

}
