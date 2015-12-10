package rabinizer.automata;

import rabinizer.ltl.Formula;
import rabinizer.ltl.GOperator;
import rabinizer.ltl.ValuationSet;
import rabinizer.ltl.ValuationSetFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Product extends GenericProduct<FormulaAutomatonState, GOperator, RankingState, FormulaAutomaton<Formula>, RabinSlave, ProductState> {

    public Product(FormulaAutomaton primaryAutomaton, Map<GOperator, RabinSlave> slaves, ValuationSetFactory<String> factory) {
        super(primaryAutomaton, slaves, factory);
    }

    public Product(Product a) {
        super(a.primaryAutomaton, a.secondaryAutomata, a.valuationSetFactory);
    }

    Set<ValuationSet> generateSuccTransitionsReflectingSinks(ProductState s) {
        Set<Set<ValuationSet>> product = new HashSet<>();
        product.add(primaryAutomaton.transitions.row(s.getPrimaryState()).keySet());
        for (GOperator slaveFormula : s.getSecondaryMap().keySet()) {
            FormulaAutomaton m = secondaryAutomata.get(slaveFormula).mojmir;
            for (Object fs : m.getStates()) {
                product.add(m.transitions.row(fs).keySet());
            }
        }
        product.removeIf(Set::isEmpty); // removing empty trans due to sinks
        return generatePartitioning(product);
    }

    @Override
    protected ProductState generateInitialState() {
        return new ProductState(primaryAutomaton.getInitialState(), relevantSecondary(primaryAutomaton.getInitialState()), k -> secondaryAutomata.get(k).getInitialState());
    }

    @Override
    protected Set<GOperator> relevantSecondary(FormulaAutomatonState primaryState) {
        return primaryState.getFormula().relevantGFormulas(secondaryAutomata.keySet());
    }

    @Override
    protected ProductState buildProductState(FormulaAutomatonState primaryState, Map<GOperator, RankingState> secondaryStates) {
        return new ProductState(primaryState, secondaryStates);
    }

}
