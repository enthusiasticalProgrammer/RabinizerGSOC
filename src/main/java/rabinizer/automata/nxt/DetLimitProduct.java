package rabinizer.automata.nxt;

import rabinizer.automata.GenericProduct;
import rabinizer.automata.Master;
import rabinizer.automata.Optimisation;
import rabinizer.ltl.*;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class DetLimitProduct extends GenericProduct<GOperator, Master.State, DetLimitSlave.State> {

    private final EquivalenceClass GConjunction;
    private final EquivalenceClassFactory equivalenceClassFactory;

    public DetLimitProduct(DetLimitMaster primaryAutomaton, Collection<GOperator> keys, Function<GOperator, DetLimitSlave> constructor, EquivalenceClassFactory factory, ValuationSetFactory<String> valuationSetFactory, Collection<Optimisation> optimisations) {
        super(primaryAutomaton, keys, constructor, valuationSetFactory, optimisations);
        GConjunction = factory.createEquivalenceClass(new Conjunction(keys));
        this.equivalenceClassFactory = factory;
    }

    public int numberOfSecondary() {
        return secondaryAutomata.size();
    }

    @Override
    protected Set<GOperator> relevantSecondary(Master.State primaryState) {
        return secondaryAutomata.keySet();
    }

    @Override
    protected State generateInitialState() {
        return generateInitialState(primaryAutomaton.getInitialState());
    }

    protected State generateInitialState(Master.State master) {
        return new State(master, secondaryAutomata.keySet(), g -> secondaryAutomata.get(g).getInitialState());
    }

    public class State extends GenericProductState {

        public State(Master.State primaryState, Map<GOperator, DetLimitSlave.State> secondaryStates) {
            super(primaryState, secondaryStates);
        }

        public State(Master.State primaryState, Collection<GOperator> keys, Function<GOperator, DetLimitSlave.State> constructor) {
            super(primaryState, keys, constructor);
        }

        @Override
        public boolean isAccepting(Set<String> valuation) {
            EquivalenceClass slaveConjunction = secondaryStates.values().stream().map(state -> state.next.and(state.current)).reduce(equivalenceClassFactory.getTrue(), EquivalenceClass::and);
            EquivalenceClass antecedent = GConjunction.and(slaveConjunction);
            return antecedent.implies(primaryState.getClazz());
        }
    }
}