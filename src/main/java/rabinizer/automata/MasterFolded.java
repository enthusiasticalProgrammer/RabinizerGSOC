package rabinizer.automata;

import rabinizer.ltl.*;

import java.util.Set;

public class MasterFolded extends FormulaAutomaton {

    public MasterFolded(Formula formula, EquivalenceClassFactory equivalenceClassFactory, ValuationSetFactory<String> valuationSetFactory) {
        super(formula, equivalenceClassFactory, valuationSetFactory);
    }

    @Override
    protected Set<ValuationSet> generateSuccTransitions(FormulaAutomatonState s) {
        return generatePartitioning(s.getFormula().unfold(true));
    }

    @Override
    protected EquivalenceClass init(EquivalenceClass clazz) {
        return clazz;
    }

    @Override
    protected EquivalenceClass step(EquivalenceClass clazz, Set<String> valuation) {
        return clazz.unfold(true).temporalStep(valuation);
    }
}
