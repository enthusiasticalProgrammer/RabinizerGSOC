package rabinizer.automata.nxt;

import rabinizer.automata.Master;
import rabinizer.automata.Optimisation;
import rabinizer.ltl.*;

import java.util.Collection;

class DetLimitMaster extends Master {
    public DetLimitMaster(Formula formula, EquivalenceClassFactory equivalenceClassFactory,
                          ValuationSetFactory valuationSetFactory, Collection<Optimisation> optimisations, boolean mergingEnabled) {
        super(formula, equivalenceClassFactory, valuationSetFactory, optimisations, mergingEnabled);
    }

    public Master.State createState(EquivalenceClass clazz) {
        return new Master.State(clazz);
    }
}
