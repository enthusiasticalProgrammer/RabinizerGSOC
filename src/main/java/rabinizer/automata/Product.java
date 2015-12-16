package rabinizer.automata;

import rabinizer.ltl.GOperator;
import rabinizer.ltl.ValuationSet;
import rabinizer.ltl.ValuationSetFactory;

import java.util.*;
import java.util.function.Function;

public class Product extends GenericProduct<GOperator, Master.State, RabinSlave.State> {

    public Product(Master primaryAutomaton, Map<GOperator, ? extends Automaton<RabinSlave.State>> slaves, ValuationSetFactory<String> factory, Collection<Optimisation> optimisations) {
        super(primaryAutomaton, slaves, factory, optimisations);
        this.trapState = new ProductState(primaryAutomaton.trapState, Collections.emptyMap());
    }

    Set<ValuationSet> generateSuccTransitionsReflectingSinks(GenericProduct<GOperator, Master.State, RabinSlave.State>.GenericProductState s) {
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

    protected GenericProduct.GenericProductState generateInitialState() {
        return new GenericProduct.GenericProductState(primaryAutomaton.getInitialState(), relevantSecondary(primaryAutomaton.getInitialState()), k -> secondaryAutomata.get(k).getInitialState());
    }

    protected Set<GOperator> relevantSecondary(Master.State primaryState) {
        return primaryState.getClazz().getRepresentative().relevantGFormulas(secondaryAutomata.keySet());
    }

    public class ProductState extends GenericProductState {

        public ProductState(Master.State primaryState, Map<GOperator, RabinSlave.State> secondaryStates) {
            super(primaryState, secondaryStates);
        }

        public ProductState(Master.State primaryState, Collection<GOperator> keys, Function<GOperator, RabinSlave.State> constructor) {
            super(primaryState, keys, constructor);
        }
    }
}
