package rabinizer.automata;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import rabinizer.ltl.ValuationSet;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractProductState<P extends IState<P>, K, S extends IState<S>, T> {

    protected final P primaryState;
    protected final Map<K, S> secondaryStates;

    protected AbstractProductState(P primaryState, Map<K, S> secondaryStates) {
        this.primaryState = primaryState;
        this.secondaryStates = secondaryStates;
    }

    protected AbstractProductState(P primaryState, Iterable<K> keys, Function<K, S> constructor) {
        this.primaryState = primaryState;

        ImmutableMap.Builder<K, S> builder = ImmutableMap.builder();
        keys.forEach(k -> builder.put(k, constructor.apply(k)));
        this.secondaryStates = builder.build();
    }

    // TODO: Drop getters?
    public P getPrimaryState() {
        return primaryState;
    }

    public S getSecondaryState(K key) {
        return secondaryStates.get(key);
    }

    public Map<K, S> getSecondaryMap() {
        return secondaryStates;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractProductState<?, ?, ?, ?> that = (AbstractProductState<?, ?, ?, ?>) o;
        return Objects.equals(primaryState, that.primaryState) &&
                Objects.equals(secondaryStates, that.secondaryStates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(primaryState, secondaryStates);
    }

    @Override
    public String toString() {
        return "(" + primaryState + "::" + secondaryStates + ')';
    }

    public T getSuccessor(Set<String> valuation) {
        P primarySuccessor = primaryState.getSuccessor(valuation);

        if (primarySuccessor == null) {
            return null;
        }

        ImmutableMap.Builder<K, S> builder = ImmutableMap.builder();

        for (K key : relevantSecondary(primarySuccessor)) {
            S secondary = secondaryStates.get(key);

            if (secondary != null) {
                S secondarySuccessor = secondary.getSuccessor(valuation);

                if (secondarySuccessor != null) {
                    builder.put(key, secondarySuccessor);
                } else {
                    return null;
                }
            }
        }

        return constructState(primarySuccessor, builder.build());
    }

    public Set<ValuationSet> partitionSuccessors() {
        List<Set<ValuationSet>> product = new ArrayList<>(secondaryStates.size() + 1);

        product.add(primaryState.partitionSuccessors());

        for (S secondaryState : secondaryStates.values()) {
            product.add(secondaryState.partitionSuccessors());
        }

        return Sets.cartesianProduct(product).stream()
                .map(this::flatten)
                .filter(set -> !set.isEmpty())
                .collect(Collectors.toSet());
    }

    protected abstract Set<K> relevantSecondary(P primaryState);

    protected abstract T constructState(P primaryState, Map<K, S> secondaryStates);

    protected abstract ValuationSet createUniverseValuationSet();

    private ValuationSet flatten(Iterable<ValuationSet> list) {
        ValuationSet flattSet = createUniverseValuationSet();
        list.forEach(flattSet::retainAll);
        return flattSet;
    }
}
