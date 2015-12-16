package rabinizer.automata;

import rabinizer.ltl.*;

import java.util.Set;

public abstract class FormulaAutomaton<L extends Formula> extends GenericFormulaAutomaton<FormulaAutomatonState, L> {

    protected FormulaAutomaton(L formula, EquivalenceClassFactory eqFactory, ValuationSetFactory<String> factory) {
        super(formula, eqFactory, factory);
        trapState = new FormulaAutomatonState(eqFactory.getFalse());
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
