package rabinizer.automata.nxt;

import com.google.common.collect.ImmutableMap;
import rabinizer.automata.AbstractProductState;
import rabinizer.automata.Automaton;
import rabinizer.automata.IState;
import rabinizer.automata.Master;
import rabinizer.ltl.*;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class DetLimitProduct extends Automaton<DetLimitProduct.State> {

    private final Master primaryAutomaton;
    private final Map<GOperator, DetLimitSlave> secondaryAutomata;

    private final EquivalenceClass GConjunction;
    private final EquivalenceClassFactory equivalenceClassFactory;

    public DetLimitProduct(DetLimitMaster primaryAutomaton, Collection<GOperator> keys, Function<GOperator, DetLimitSlave> constructor, EquivalenceClassFactory factory, ValuationSetFactory<String> valuationSetFactory) {
        super(valuationSetFactory);
        this.primaryAutomaton = primaryAutomaton;

        ImmutableMap.Builder<GOperator, DetLimitSlave> builder = ImmutableMap.builder();
        keys.forEach(k -> builder.put(k, constructor.apply(k)));
        secondaryAutomata = builder.build();

        GConjunction = factory.createEquivalenceClass(new Conjunction(keys));
        equivalenceClassFactory = factory;
    }

    public int numberOfSecondary() {
        return secondaryAutomata.size();
    }

    @Override
    protected DetLimitProduct.State generateInitialState() {
        return generateInitialState(primaryAutomaton.getInitialState());
    }

    protected DetLimitProduct.State generateInitialState(Master.State master) {
        return new State(master, secondaryAutomata.keySet(), g -> secondaryAutomata.get(g).getInitialState());
    }

    public class State extends AbstractProductState<Master.State, GOperator, DetLimitSlave.State, State> implements IState<DetLimitProduct.State> {

        public State(Master.State primaryState, Map<GOperator, DetLimitSlave.State> secondaryStates) {
            super(primaryState, secondaryStates);
        }

        public State(Master.State primaryState, Collection<GOperator> keys, Function<GOperator, DetLimitSlave.State> constructor) {
            super(primaryState, keys, constructor);
        }

        @Override
        public boolean isAccepting(Set<String> valuation) {
            EquivalenceClass slaveConjunction = secondaryStates.values().stream().map(s -> s.next.and(s.current)).reduce(equivalenceClassFactory.getTrue(), EquivalenceClass::and);
            EquivalenceClass antecedent = GConjunction.and(slaveConjunction);
            return antecedent.implies(primaryState.getClazz());
        }

        @Override
        protected Set<GOperator> relevantSecondary(Master.State primaryState) {
            return secondaryAutomata.keySet();
        }

        @Override
        protected DetLimitProduct.State constructState(Master.State primaryState, Map<GOperator, DetLimitSlave.State> secondaryStates) {
            return new State(primaryState, secondaryStates);
        }

        @Override
        protected ValuationSet createUniverseValuationSet() {
            return valuationSetFactory.createUniverseValuationSet();
        }
    }
}