package rabinizer.automata;

import rabinizer.ltl.*;

import java.util.Set;

public abstract class FormulaAutomaton extends GenericFormulaAutomaton<FormulaAutomatonState> {

    protected FormulaAutomaton(Formula formula, EquivalenceClassFactory eqFactory, ValuationSetFactory<String> factory) {
        super(formula, eqFactory, factory);
    }

    @Override
    public FormulaAutomatonState generateInitialState() {
        return new FormulaAutomatonState(init(initial));
    }

    @Override
    public FormulaAutomatonState generateSuccState(FormulaAutomatonState s, ValuationSet vs) {
        return new FormulaAutomatonState(step(s.getEquivalenceClass(), vs.pickAny()));
    }

    @Override
    protected Set<ValuationSet> generateSuccTransitions(FormulaAutomatonState s) {
        return generatePartitioning(s.getFormula());
    }

    protected abstract EquivalenceClass init(EquivalenceClass clazz);

    protected abstract EquivalenceClass step(EquivalenceClass clazz, Set<String> valuation);
}

