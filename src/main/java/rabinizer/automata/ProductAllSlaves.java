package rabinizer.automata;

import rabinizer.ltl.Formula;
import rabinizer.ltl.GOperator;
import rabinizer.ltl.ValuationSetFactory;

import java.util.Map;
import java.util.Set;

public class ProductAllSlaves extends Product {

    public ProductAllSlaves(FormulaAutomaton<Formula> master, Map<GOperator, RabinSlave> slaves, ValuationSetFactory<String> factory) {
        super(master, slaves, factory);
    }

    @Override
    protected Set<GOperator> relevantSecondary(FormulaAutomatonState primaryState) {
        return secondaryAutomata.keySet();
    }
}
