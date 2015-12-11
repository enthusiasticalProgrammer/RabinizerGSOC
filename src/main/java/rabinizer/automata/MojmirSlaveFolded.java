package rabinizer.automata;

import rabinizer.ltl.*;

import java.util.Set;

public class MojmirSlaveFolded extends FormulaAutomaton<GOperator> {

    public MojmirSlaveFolded(GOperator formula, EquivalenceClassFactory eqFactory, ValuationSetFactory<String> factory) {
        super(formula, eqFactory, factory);
    }

    @Override
    protected Set<ValuationSet> generateSuccTransitions(FormulaAutomatonState s) {
        return generatePartitioning(s.getFormula().unfold(false));
    }

    @Override
    protected EquivalenceClass init(EquivalenceClass clazz) {
        return clazz;
    }

    @Override
    protected EquivalenceClass step(EquivalenceClass clazz, Set<String> valuation) {
        return clazz.unfold(false).temporalStep(valuation);
    }
}