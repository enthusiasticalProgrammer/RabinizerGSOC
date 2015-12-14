package rabinizer.automata.nxt;

import rabinizer.automata.FormulaAutomaton;
import rabinizer.automata.FormulaAutomatonState;
import rabinizer.ltl.*;

import java.util.Collection;
import java.util.Set;

public class DetLimitMaster extends FormulaAutomaton<Formula> {
    public DetLimitMaster(Formula formula, EquivalenceClassFactory eqFactory, ValuationSetFactory<String> factory) {
        super(formula, eqFactory, factory);
    }

    public boolean isAcceptingTransition(FormulaAutomatonState s, ValuationSet vs, Collection<GOperator> Gs, Collection<DetLimitSlaveState> slaveStates) {
        EquivalenceClass GClass = factory.createEquivalenceClass(new Conjunction(Gs));
        EquivalenceClass antecedent = GClass.and(slaveStates.stream().map(slaveState -> slaveState.next.and(slaveState.current)).reduce(factory.getTrue(), EquivalenceClass::and));
        EquivalenceClass consequent = s.getEquivalenceClass();
        return antecedent.implies(consequent);
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
