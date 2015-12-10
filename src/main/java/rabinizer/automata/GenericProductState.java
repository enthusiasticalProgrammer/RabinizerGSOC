package rabinizer.automata;

import com.google.common.collect.ImmutableMap;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class GenericProductState<P, K, S> {

    private final P primaryState;
    private final Map<K, S> secondaryStates;

    public GenericProductState(P primaryState, Map<K, S> secondaryStates) {
        this.primaryState = primaryState;
        this.secondaryStates = secondaryStates;
    }

    public GenericProductState(P primaryState, Collection<K> keys, Function<K, S> constructor) {
        this.primaryState = primaryState;

        ImmutableMap.Builder<K, S> builder = ImmutableMap.builder();
        keys.forEach(k -> builder.put(k, constructor.apply(k)));
        this.secondaryStates = builder.build();
    }

    public P getPrimaryState() {
        return primaryState;
    }

    public S getSecondaryState(K key) {
        return secondaryStates.get(key);
    }

    public Collection<S> getSecondaryStates() {
        return secondaryStates.values();
    }

    public Map<K, S> getSecondaryMap() {
        return secondaryStates;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GenericProductState<?, ?, ?> that = (GenericProductState<?, ?, ?>) o;
        return Objects.equals(primaryState, that.primaryState) &&
                Objects.equals(secondaryStates, that.secondaryStates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(primaryState, secondaryStates);
    }

    @Override
    public String toString() {
        return "(" + primaryState + "::" + secondaryStates + ")";
    }
}
