package rabinizer.automata;

import rabinizer.ltl.*;

import java.util.*;

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
        Optional<Literal> l = f.getAnUnguardedLiteral();

        if (!l.isPresent()) {
            result.add(valuationSetFactory.createUniverseValuationSet());
        } else {
            Literal literal = l.get().positiveLiteral();
            //System.out.println("  gen " + f + "; " + l);
            Set<ValuationSet> pos = generatePartitioning(f.evaluate(literal));
            Set<ValuationSet> neg = generatePartitioning(f.evaluate(literal.not()));
            for (ValuationSet vs : pos) {
                vs.restrictWith(literal);
                result.add(vs);
            }
            for (ValuationSet vs : neg) {
                vs.restrictWith(literal.not());
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
