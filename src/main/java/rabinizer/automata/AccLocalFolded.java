/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rabinizer.automata;

import rabinizer.ltl.bdd.GSet;
import rabinizer.ltl.bdd.Valuation;
import rabinizer.ltl.bdd.ValuationSetBDD;
import rabinizer.ltl.Formula;
import rabinizer.ltl.FormulaFactory;

import java.util.Map;

/**
 * @author jkretinsky
 */
public class AccLocalFolded extends AccLocal {

    public AccLocalFolded(Product product) {
        super(product);
    }

    protected static boolean slavesEntail(GSet gSet, GSet gSetComplement, ProductState ps, Map<Formula, Integer> ranking, Valuation v, Formula consequent) {
        Formula antecedent = FormulaFactory.mkConst(true);
        for (Formula f : gSet) {
            antecedent = FormulaFactory.mkAnd(antecedent, FormulaFactory.mkG(f)); // TODO relevant for Folded version
            //antecedent = new Conjunction(antecedent, new XOperator(new GOperator(f))); // TODO:remove; relevant for Xunfolding
            Formula slaveAntecedent = FormulaFactory.mkConst(true);
            if (ps.containsKey(f)) {
                for (FormulaState s : ps.get(f).keySet()) {
                    if (ps.get(f).get(s) >= ranking.get(f)) {
                        slaveAntecedent = FormulaFactory.mkAnd(slaveAntecedent, s.formula);
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
        TranSet<ProductState> result = new TranSet<>();
        if (!slavesEntail(gSet, gSetComplement, ps, ranking, null, ps.masterState.formula)) {
            result.add(ps, ValuationSetBDD.getAllVals());
        }
        return result;
    }

}
