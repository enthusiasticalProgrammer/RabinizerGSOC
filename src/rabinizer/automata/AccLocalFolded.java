/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rabinizer.automata;

import rabinizer.bdd.GSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import rabinizer.bdd.Valuation;
import rabinizer.bdd.ValuationSetBDD;
import rabinizer.formulas.*;

/**
 *
 * @author jkretinsky
 */
public class AccLocalFolded extends AccLocal {

    public AccLocalFolded(Product product) {
        super(product);
    }

    @Override
    protected TranSet<ProductState> computeAccMasterForState(GSet gSet, GSet gSetComplement, Map<Formula, Integer> ranking, ProductState ps) {
        TranSet<ProductState> result = new TranSet<ProductState>();
        if (!slavesEntail(gSet, gSetComplement, ps, ranking, null, ps.masterState.formula)) {
            result.add(ps, ValuationSetBDD.getAllVals());
        }
        return result;
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

}
