package rabinizer.automata;

import com.google.common.collect.ImmutableMap;
import rabinizer.ltl.ValuationSet;
import rabinizer.ltl.ValuationSetFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public abstract class GenericProduct<P, K, S, AP extends Automaton<P>, AS extends Automaton<S>, PS extends GenericProductState<P, K, S>> extends Automaton<PS> {

    protected final AP primaryAutomaton;
    protected final Map<K, AS> secondaryAutomata;

    public GenericProduct(AP primaryAutomaton, Map<K, AS> secondaryAutomata, ValuationSetFactory<String> valuationSetFactory) {
        super(valuationSetFactory);
        this.primaryAutomaton = primaryAutomaton;
        this.secondaryAutomata = ImmutableMap.copyOf(secondaryAutomata);
    }

    public GenericProduct(AP primaryAutomaton, Collection<K> keys, Function<K, AS> constructor, ValuationSetFactory<String> valuationSetFactory) {
        super(valuationSetFactory);
        this.primaryAutomaton = primaryAutomaton;

        ImmutableMap.Builder<K, AS> builder = ImmutableMap.builder();
        keys.forEach(k -> builder.put(k, constructor.apply(k)));
        this.secondaryAutomata = builder.build();
    }

    protected abstract Set<K> relevantSecondary(P primaryState);

    protected abstract PS buildProductState(P primaryState, Map<K, S> secondaryStates);

    //TODO: computing labels after construction would be faster
    @Override
    protected PS generateSuccState(PS s, ValuationSet vs) {
        Set<String> valuation = vs.pickAny();

        P primarySuccessor = primaryAutomaton.succ(s.getPrimaryState(), valuation);

        ImmutableMap.Builder<K, S> builder = ImmutableMap.builder();

        for (K key : relevantSecondary(primarySuccessor)) {
            S secondary = s.getSecondaryState(key);

            if (secondary != null) {
                builder.put(key, secondaryAutomata.get(key).succ(secondary, valuation));
            } else {
                builder.put(key, secondaryAutomata.get(key).getInitialState());
            }
        }

        return buildProductState(primarySuccessor, builder.build());
    }

    @Override
    protected Set<ValuationSet> generateSuccTransitions(PS s) {
        Set<Set<ValuationSet>> product = new HashSet<>();

        product.add(primaryAutomaton.transitions.row(s.getPrimaryState()).keySet());

        for (Map.Entry<K, S> secondaryEntry : s.getSecondaryMap().entrySet()) {
            product.add(secondaryAutomata.get(secondaryEntry.getKey()).transitions.row(secondaryEntry.getValue()).keySet());
        }

        return generatePartitioning(product);
    }
}
