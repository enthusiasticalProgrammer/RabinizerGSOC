/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rabinizer.automata;

/**
 * @author jkretinsky
 */

import rabinizer.ltl.EquivalenceClassFactory;
import rabinizer.ltl.Formula;
import rabinizer.ltl.ValuationSet;
import rabinizer.ltl.ValuationSetFactory;

import java.util.Set;

public class MasterFolded extends FormulaAutomaton {

    public MasterFolded(Formula formula, EquivalenceClassFactory equivalenceClassFactory, ValuationSetFactory<String> valuationSetFactory) {
        super(formula, equivalenceClassFactory, valuationSetFactory);
    }

    @Override
    public FormulaState generateInitialState() {
        return new FormulaState(equivalenceClassFactory.createEquivalenceClass(formula));
    }

    @Override
    public FormulaState generateSuccState(FormulaState s, ValuationSet vs) {
        Formula succ = s.getFormula().unfold(true).temporalStep(vs.pickAny()); // any element of the equivalence class
        return new FormulaState(equivalenceClassFactory.createEquivalenceClass(succ));
    }

    @Override
    protected Set<ValuationSet> generateSuccTransitions(FormulaState s) {
        return generatePartitioning(s.getFormula().unfold(true));
    }

}
