package rabinizer.automata;

import rabinizer.ltl.EquivalenceClass;
import rabinizer.ltl.EquivalenceClassFactory;
import rabinizer.ltl.Formula;
import rabinizer.ltl.ValuationSetFactory;

import java.util.Set;

public class Master extends FormulaAutomaton<Formula> {

    public Master(Formula formula, EquivalenceClassFactory equivalenceClassFactory,
            ValuationSetFactory<String> valuationSetFactory) {
        super(formula, equivalenceClassFactory, valuationSetFactory);
    }

    @Override
    protected EquivalenceClass init(EquivalenceClass clazz) {
        return clazz.unfold(true);
    }

    @Override
    protected EquivalenceClass step(EquivalenceClass clazz, Set<String> valuation) {
        return clazz.temporalStep(valuation).unfold(true);
                                                                   // element of
                                                                   // the
                                                                   // equivalence
                                                                   // class
    }
}
