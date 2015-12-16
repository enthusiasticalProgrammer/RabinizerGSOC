package rabinizer.automata.nxt;

import rabinizer.automata.Master;
import rabinizer.automata.Optimisation;
import rabinizer.ltl.EquivalenceClassFactory;
import rabinizer.ltl.Formula;
import rabinizer.ltl.ValuationSetFactory;

import java.util.Collection;

public class DetLimitMaster extends Master {
    public DetLimitMaster(Formula formula, EquivalenceClassFactory equivalenceClassFactory,
                          ValuationSetFactory<String> valuationSetFactory, Collection<Optimisation> optimisations) {
        super(formula, equivalenceClassFactory, valuationSetFactory, optimisations);
    }
}
