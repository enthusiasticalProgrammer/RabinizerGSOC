package rabinizer.automata;

import rabinizer.ltl.EquivalenceClass;
import rabinizer.ltl.EquivalenceClassFactory;
import rabinizer.ltl.Formula;
import rabinizer.ltl.ValuationSetFactory;

import java.util.Set;

public class MojmirSlave extends FormulaAutomaton {

    public MojmirSlave(Formula formula, EquivalenceClassFactory eqFactory, ValuationSetFactory<String> factory) {
        super(formula, eqFactory, factory);
    }

    @Override
    protected EquivalenceClass init(EquivalenceClass clazz) {
        return clazz.unfold(false);
    }

    @Override
    protected EquivalenceClass step(EquivalenceClass clazz, Set<String> valuation) {
        return clazz.temporalStep(valuation).unfold(false);
    }
}
