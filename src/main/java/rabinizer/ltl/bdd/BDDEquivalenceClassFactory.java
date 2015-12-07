package rabinizer.ltl.bdd;

import rabinizer.ltl.EquivalenceClassFactory;
import rabinizer.ltl.Formula;

import java.util.Set;

public class BDDEquivalenceClassFactory extends BDDLibraryWrapper<Formula> implements EquivalenceClassFactory {

    public BDDEquivalenceClassFactory(Set<Formula> domain) {
        super(domain);
    }

    @Override
    public BDDEquivalenceClass createEquivalenceClass(Formula formula) {
        return new BDDEquivalenceClass(formula, createBDD(formula), this);
    }
}
