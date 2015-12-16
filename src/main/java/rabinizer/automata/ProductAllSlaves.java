package rabinizer.automata;

import rabinizer.ltl.GOperator;
import rabinizer.ltl.ValuationSetFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class ProductAllSlaves extends Product {
    public ProductAllSlaves(Master primaryAutomaton, Map<GOperator, RabinSlave> slaves, ValuationSetFactory<String> factory, Collection<Optimisation> optimisations) {
        super(primaryAutomaton, slaves, factory, optimisations);
    }

    @Override
    protected Set<GOperator> relevantSecondary(Master.State primaryState) {
        return secondaryAutomata.keySet();
    }
}
