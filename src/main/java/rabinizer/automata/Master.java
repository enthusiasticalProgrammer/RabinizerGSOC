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

public class Master extends FormulaAutomaton {

    public Master(Formula formula, EquivalenceClassFactory equivalenceClassFactory, ValuationSetFactory<String> valuationSetFactory) {
        super(formula, equivalenceClassFactory, valuationSetFactory);
    }

    @Override
    public FormulaState generateInitialState() {
        FormulaState init = new FormulaState(equivalenceClassFactory.createEquivalenceClass(formula.unfold(true)));
        stateLabels.put(init, formula);
        return init;
    }

    @Override
    public FormulaState generateSuccState(FormulaState s, ValuationSet vs) {
        Formula label = s.getFormula().temporalStep(vs.pickAny()); // any element of the equivalence class
        FormulaState state = new FormulaState(equivalenceClassFactory.createEquivalenceClass(label.unfold(true)));
        if (!states.contains(state)) {
            stateLabels.put(state, label);
        }
        return state;
    }

}
