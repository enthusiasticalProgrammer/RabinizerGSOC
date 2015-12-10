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

    protected boolean slavesEntail(Set<GOperator> gSet, ProductState ps, Map<Formula, Integer> ranking, Set<String> v, Formula consequent) {
        Formula antecedent = BooleanConstant.get(true);
        for (GOperator f : gSet) {
            antecedent = FormulaFactory.mkAnd(antecedent, f); // TODO relevant for Folded version
            //antecedent = new Conjunction(antecedent, new XOperator(new GOperator(f))); // TODO:remove; relevant for Xunfolding
            Formula slaveAntecedent = BooleanConstant.get(true);
            if (ps.getSecondaryState(f) != null) {
                for (FormulaAutomatonState s : ps.getSecondaryState(f).keySet()) {
                    if (ps.getSecondaryState(f).get(s) >= ranking.get(f)) {
                        slaveAntecedent = FormulaFactory.mkAnd(slaveAntecedent, s.getFormula());
                    }
                }
            }
            slaveAntecedent = slaveAntecedent.evaluate(gSet);
            antecedent = FormulaFactory.mkAnd(antecedent, slaveAntecedent);
        }
        return entails(antecedent, consequent);
    }

    @Override
    protected TranSet<ProductState> computeAccMasterForState(Set<GOperator> gSet, Map<Formula, Integer> ranking, ProductState ps) {
        TranSet<ProductState> result = new TranSet<>(valuationSetFactory);
        if (!slavesEntail(gSet, ps, ranking, null, ps.getPrimaryState().getFormula())) {
            result.add(ps, valuationSetFactory.createUniverseValuationSet());
        }
        return result;
    }

}
