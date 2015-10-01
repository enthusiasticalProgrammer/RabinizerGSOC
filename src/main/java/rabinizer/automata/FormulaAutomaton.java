/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rabinizer.automata;

import rabinizer.ltl.bdd.BDDForVariables;
import rabinizer.ltl.bdd.ValuationSet;
import rabinizer.ltl.bdd.ValuationSetBDD;
import rabinizer.ltl.Formula;
import rabinizer.ltl.Literal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author jkretinsky
 */
public abstract class FormulaAutomaton extends Automaton<FormulaState> {

    public Formula formula;
    protected Map<FormulaState, Formula> stateLabels;

    public FormulaAutomaton(Formula formula) {
        super();
        this.formula = formula;
        stateLabels = new HashMap<>();
    }

    protected static Set<ValuationSet> generatePartitioning(Formula f) { // TODO
                                                                         // method
                                                                         // of
                                                                         // state
        Set<ValuationSet> result = new HashSet<>();
        Literal l = f.getAnUnguardedLiteral();
        if (l == null) {
            result.add(new ValuationSetBDD(BDDForVariables.getTrueBDD()));
        } else {
            l = l.positiveLiteral();
            // System.out.println(" gen " + f + "; " + l);
            Set<ValuationSet> pos = generatePartitioning(f.assertLiteral(l));
            Set<ValuationSet> neg = generatePartitioning(f.assertLiteral(l.not()));
            for (ValuationSet vs : pos) {
                result.add(vs.and(l));
            }
            for (ValuationSet vs : neg) {
                result.add(vs.and(l.not()));
            }
        }
        return result;
    }

    @Override
    protected Set<ValuationSet> generateSuccTransitions(FormulaState s) {
        return generatePartitioning(s.formula);
    }

}
