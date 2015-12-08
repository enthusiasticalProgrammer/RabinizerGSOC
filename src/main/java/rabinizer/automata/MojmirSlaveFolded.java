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

public class MojmirSlaveFolded extends FormulaAutomaton {

    public MojmirSlaveFolded(Formula formula, EquivalenceClassFactory eqFactory, ValuationSetFactory<String> factory) {
        super(formula, eqFactory, factory);
    }

    @Override
    public FormulaState generateInitialState() {
        return new FormulaState(equivalenceClassFactory.createEquivalenceClass(formula));
    }

    @Override
    public FormulaState generateSuccState(FormulaState s, ValuationSet vs) {
        Formula succ = s.getFormula().unfold(false).temporalStep(vs.pickAny());
        return new FormulaState(equivalenceClassFactory.createEquivalenceClass(formula));
    }

    @Override
    protected Set<ValuationSet> generateSuccTransitions(FormulaState s) {
        return generatePartitioning(s.getFormula().unfold(false));
    }

}
