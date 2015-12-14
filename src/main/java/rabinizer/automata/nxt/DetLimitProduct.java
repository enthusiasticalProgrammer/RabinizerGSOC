package rabinizer.automata.nxt;

import rabinizer.automata.Automaton;
import rabinizer.automata.FormulaAutomatonState;
import rabinizer.automata.GenericProduct;
import rabinizer.ltl.GOperator;
import rabinizer.ltl.ValuationSet;
import rabinizer.ltl.ValuationSetFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class DetLimitProduct extends GenericProduct<FormulaAutomatonState, GOperator, DetLimitSlaveState, DetLimitMaster, DetLimitSlave, DetLimitProductState> {

    public DetLimitProduct(DetLimitMaster primaryAutomaton, Collection<GOperator> keys, Function<GOperator, DetLimitSlave> constructor, ValuationSetFactory<String> valuationSetFactory) {
        super(primaryAutomaton, keys, constructor, valuationSetFactory);
    }

    public int numberOfSecondary() {
        return secondaryAutomata.size();
    }

    @Override
    protected Set<GOperator> relevantSecondary(FormulaAutomatonState primaryState) {
        return secondaryAutomata.keySet();
    }

    @Override
    protected DetLimitProductState buildProductState(FormulaAutomatonState primaryState, Map<GOperator, DetLimitSlaveState> secondaryStates) {
        return new DetLimitProductState(primaryState, secondaryStates);
    }

    @Override
    protected DetLimitProductState generateInitialState() {
        return generateInitialState(primaryAutomaton.getInitialState());
    }

    protected DetLimitProductState generateInitialState(FormulaAutomatonState master) {
        secondaryAutomata.values().forEach(Automaton::generate);
        return new DetLimitProductState(master, secondaryAutomata.keySet(), g -> secondaryAutomata.get(g).generateInitialState());
    }

    @Override
    protected Set<ValuationSet> generateSuccTransitions(DetLimitProductState s) {
        return valuationSetFactory.createAllValuationSets();
    }
}