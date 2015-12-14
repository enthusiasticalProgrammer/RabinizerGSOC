package rabinizer.ltl.bdd;

import rabinizer.ltl.BooleanConstant;
import rabinizer.ltl.EquivalenceClass;
import rabinizer.ltl.EquivalenceClassFactory;
import rabinizer.ltl.Formula;

import java.util.Set;

public class BDDEquivalenceClassFactory extends BDDLibraryWrapper<Formula> implements EquivalenceClassFactory {

    public BDDEquivalenceClassFactory(Set<Formula> domain) {
        super(domain);
    }

    @Override
    public EquivalenceClass getTrue() {
        return new BDDEquivalenceClass(BooleanConstant.TRUE, factory.one(), this);
    }

    @Override
    public EquivalenceClass getFalse() {
        return new BDDEquivalenceClass(BooleanConstant.FALSE, factory.zero(), this);
    }

    @Override
    public BDDEquivalenceClass createEquivalenceClass(Formula formula) {
        return new BDDEquivalenceClass(formula, createBDD(formula), this);
    }
}
