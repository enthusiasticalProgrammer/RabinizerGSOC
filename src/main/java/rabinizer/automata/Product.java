package rabinizer.automata;

import rabinizer.ltl.GOperator;
import rabinizer.ltl.ValuationSet;
import rabinizer.ltl.ValuationSetFactory;

import java.util.*;
import java.util.function.Function;

public class Product extends Automaton<Product.ProductState> {

    protected final Master primaryAutomaton;
    protected final Map<GOperator, RabinSlave> secondaryAutomata;

    protected final boolean allSlaves;

        super(factory);
    public Product(Master primaryAutomaton, Map<GOperator, RabinSlave> slaves, ValuationSetFactory factory, Collection<Optimisation> optimisations) {
        this.primaryAutomaton = primaryAutomaton;
        this.secondaryAutomata = slaves;
        this.trapState = new ProductState(primaryAutomaton.trapState, Collections.emptyMap());
        this.allSlaves = optimisations.contains(Optimisation.ALL_SLAVES);
    }

    Set<ValuationSet> generateSuccTransitionsReflectingSinks(ProductState s) {
        Set<Set<ValuationSet>> product = new HashSet<>();

        product.add(primaryAutomaton.transitions.row(s.getPrimaryState()).keySet());

        for (GOperator slaveFormula : s.getSecondaryMap().keySet()) {
            Automaton<RabinSlave.State> m = secondaryAutomata.get(slaveFormula);
            for (RabinSlave.State fs : m.getStates()) {
                product.add(m.transitions.row(fs).keySet());
            }
        }

        product.removeIf(Set::isEmpty); // removing empty trans due to sinks
        return generatePartitioning(product);
    }

    protected Product.ProductState generateInitialState() {
        return new ProductState(primaryAutomaton.getInitialState(), secondaryAutomata.keySet(), k -> secondaryAutomata.get(k).getInitialState());
    }

    public class ProductState extends AbstractProductState<Master.State, GOperator, RabinSlave.State, ProductState> implements IState<ProductState> {

        public ProductState(Master.State primaryState, Map<GOperator, RabinSlave.State> secondaryStates) {
            super(primaryState, secondaryStates);
        }

        @Override
        protected Automaton<Master.State> getPrimaryAutomaton() {
            return primaryAutomaton;
        }

        @Override
        protected Map<GOperator, RabinSlave> getSecondaryAutomata() {
            return secondaryAutomata;
        }

        public ProductState(Master.State primaryState, Collection<GOperator> keys, Function<GOperator, RabinSlave.State> constructor) {
            super(primaryState, keys, constructor);
        }

        @Override
        protected Set<GOperator> relevantSecondary(Master.State primaryState) {
            if (allSlaves) {
                return secondaryStates.keySet();
            } else {
                Set<GOperator> keys = new HashSet<>();
                primaryState.getClazz().getSupport().forEach(f -> keys.addAll(f.gSubformulas()));
                return keys;
            }
        }

        @Override
        protected ProductState constructState(Master.State primaryState, Map<GOperator, RabinSlave.State> secondaryStates) {
            return new ProductState(primaryState, secondaryStates);
        }

        @Override
        protected ValuationSet createUniverseValuationSet() {
            return valuationSetFactory.createUniverseValuationSet();
        }

        @Override
        public ValuationSetFactory getFactory() {
            return valuationSetFactory;
        }
    }
}
