package rabinizer.automata;

import org.jetbrains.annotations.NotNull;
import rabinizer.ltl.GOperator;
import rabinizer.ltl.RelevantGFormulaeWithSlaveSuspension;
import rabinizer.ltl.ValuationSet;
import rabinizer.ltl.ValuationSetFactory;

import java.util.*;
import java.util.function.Function;


public class Product extends Automaton<Product.ProductState> {

    protected final Master primaryAutomaton;
    protected final Map<GOperator, RabinSlave> secondaryAutomata;

    protected final boolean allSlaves;
    protected final boolean slaveSuspension;

    public Product(Master primaryAutomaton, Map<GOperator, RabinSlave> slaves, ValuationSetFactory factory, Collection<Optimisation> optimisations) {
        super(factory);
        // relevant secondaryAutomata dynamically
        // computed from primaryAutomaton formula
        // master formula
        this.primaryAutomaton = primaryAutomaton;
        this.secondaryAutomata = slaves;
        this.allSlaves = !optimisations.contains(Optimisation.ONLY_RELEVANT_SLAVES);
        this.slaveSuspension = optimisations.contains(Optimisation.SLAVE_SUSPENSION);
        this.trapState = new ProductState(primaryAutomaton.trapState, Collections.emptyMap());
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

    @Override
    protected @NotNull Product.ProductState generateInitialState() {
        return new ProductState(primaryAutomaton.getInitialState(), relevantSecondarySlaves(primaryAutomaton.getInitialState(), Collections.emptySet()),
                k -> secondaryAutomata.get(k).getInitialState());
    }


    private Set<GOperator> relevantSecondarySlaves(@NotNull Master.State primaryState, @NotNull Set<GOperator> parentKeys) {
        Set<GOperator> keys;
        if (allSlaves) {
            keys = secondaryAutomata.keySet();
        } else {
            keys = new HashSet<>();
            primaryState.getClazz().getSupport().forEach(f -> keys.addAll(f.gSubformulas()));
        }

        if (slaveSuspension) {
            boolean hastySlavesPresent = primaryState.getClazz().getRepresentative().accept(new RelevantGFormulaeWithSlaveSuspension());
            if (!hastySlavesPresent && parentKeys.isEmpty()) {
                return Collections.emptySet();
            }
        }
        return keys;

    }

    public class ProductState extends AbstractProductState<Master.State, GOperator, RabinSlave.State, ProductState> implements IState<ProductState> {

        private ProductState(Master.State primaryState, Map<GOperator, RabinSlave.State> secondaryStates) {
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

        private ProductState(Master.State primaryState, Collection<GOperator> keys, Function<GOperator, RabinSlave.State> constructor) {
            super(primaryState, keys, constructor);
        }

        @Override
        protected Set<GOperator> relevantSecondary(Master.State primaryState) {
            return relevantSecondarySlaves(primaryState, this.secondaryStates.keySet());
        }

        @Override
        protected ProductState constructState(Master.State primaryState, Map<GOperator, RabinSlave.State> secondaryStates) {
            return new ProductState(primaryState, secondaryStates);
        }

        @Override
        public ValuationSetFactory getFactory() {
            return valuationSetFactory;
        }
    }
}
