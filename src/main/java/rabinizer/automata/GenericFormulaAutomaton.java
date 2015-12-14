package rabinizer.automata;

import rabinizer.ltl.*;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public abstract class GenericFormulaAutomaton<S, L extends Formula> extends Automaton<S> {

    final protected EquivalenceClass initial;
    final protected L formulaLabel;
    final protected EquivalenceClassFactory factory;

    protected GenericFormulaAutomaton(L formula, EquivalenceClassFactory eqFactory,
            ValuationSetFactory<String> factory) {
        super(factory);
        formulaLabel = formula;
        this.factory = eqFactory;
        initial = eqFactory.createEquivalenceClass(formula);
    }

    protected Set<ValuationSet> generatePartitioning(Formula f) { // TODO method
                                                                  // of state
        Set<ValuationSet> result = new HashSet<>();
        Optional<Literal> l = f.getAnUnguardedLiteral();

        if (!l.isPresent()) {
            result.add(valuationSetFactory.createUniverseValuationSet());
        } else {
            Literal literal = l.get().positiveLiteral();
            // System.out.println(" gen " + f + "; " + l);
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

    public L getFormula() {
        return formulaLabel;
    }
}