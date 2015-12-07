/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rabinizer.automata;

import rabinizer.ltl.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author jkretinsky
 */
public abstract class FormulaAutomaton extends Automaton<FormulaState> {

    final protected Formula formula;
    final protected Map<FormulaState, Formula> stateLabels;
    final protected EquivalenceClassFactory equivalenceClassFactory ;

    public FormulaAutomaton(Formula formula, EquivalenceClassFactory eqFactory, ValuationSetFactory<String> factory) {
        super(factory);
        this.formula = formula;
        this.equivalenceClassFactory = eqFactory;
        this.stateLabels = new HashMap<>();
    }

    protected Set<ValuationSet> generatePartitioning(Formula f) { // TODO method of state
        Set<ValuationSet> result = new HashSet<>();
        Literal l = f.getAnUnguardedLiteral();
        if (l == null) {
            result.add(valuationSetFactory.createUniverseValuationSet());
        } else {
            l = l.positiveLiteral();
            //System.out.println("  gen " + f + "; " + l);
            Set<ValuationSet> pos = generatePartitioning(f.assertLiteral(l));
            Set<ValuationSet> neg = generatePartitioning(f.assertLiteral(l.not()));
            for (ValuationSet vs : pos) {
                vs.restrictWith(l);
                result.add(vs);
            }
            for (ValuationSet vs : neg) {
                vs.restrictWith(l.not());
                result.add(vs);
            }
        }
        return result;
    }

    @Override
    protected Set<ValuationSet> generateSuccTransitions(FormulaState s) {
        return generatePartitioning(s.getFormula());
    }

    public Formula getFormula() {
        return formula;
    }
}
