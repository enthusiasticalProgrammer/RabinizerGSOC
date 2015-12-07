/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rabinizer.automata;

import rabinizer.ltl.*;

/**
 * @author jkretinsky
 */
public class MojmirSlave extends FormulaAutomaton {

    public MojmirSlave(Formula formula, EquivalenceClassFactory eqFactory, ValuationSetFactory<String> factory) {
        super(formula, eqFactory, factory);
    }

    @Override
    public FormulaState generateInitialState() {
        EquivalenceClass clazz = equivalenceClassFactory.createEquivalenceClass(formula.unfoldNoG());
        FormulaState init = new FormulaState(clazz);
        stateLabels.put(init, formula);
        return init;
    }

    @Override
    public FormulaState generateSuccState(FormulaState s, ValuationSet vs) {
        Formula label = s.getFormula().temporalStep(vs.pickAny()); // any element of the equivalence class
        EquivalenceClass clazz = equivalenceClassFactory.createEquivalenceClass(label.unfoldNoG());
        FormulaState state = new FormulaState(clazz);

        if (!states.contains(state)) {
            stateLabels.put(state, label);
        }

        return state;
    }

}
