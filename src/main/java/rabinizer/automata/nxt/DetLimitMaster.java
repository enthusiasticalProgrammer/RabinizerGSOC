package rabinizer.automata.nxt;

import rabinizer.automata.Master;
import rabinizer.automata.Optimisation;
import rabinizer.ltl.BooleanConstant;
import rabinizer.ltl.EquivalenceClass;
import rabinizer.ltl.EquivalenceClassFactory;
import rabinizer.ltl.ValuationSetFactory;

import java.util.Collection;

class DetLimitMaster extends Master {
    public DetLimitMaster(EquivalenceClassFactory equivalenceClassFactory,
                          ValuationSetFactory valuationSetFactory, Collection<Optimisation> optimisations, boolean mergingEnabled) {
        super(BooleanConstant.TRUE, equivalenceClassFactory, valuationSetFactory, optimisations, mergingEnabled);
    }

    public Master.State createState(EquivalenceClass clazz) {
        return new Master.State(clazz);
    }
}
