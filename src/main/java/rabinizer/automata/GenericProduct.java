package rabinizer.automata;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import rabinizer.ltl.ValuationSet;
import rabinizer.ltl.ValuationSetFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class GenericProduct<K, P extends IState<P>, S extends IState<S>> extends Automaton<GenericProduct<K, P, S>.GenericProductState> {

    protected final Automaton<P> primaryAutomaton;
    protected final Map<K, Automaton<S>> secondaryAutomata;
    private final boolean fastTrap;

    public GenericProduct(Automaton<P> primaryAutomaton, Map<K, ? extends Automaton<S>> secondaryAutomata, ValuationSetFactory<String> valuationSetFactory, Collection<Optimisation> optimisations) {
        super(valuationSetFactory);
        this.primaryAutomaton = primaryAutomaton;
        this.secondaryAutomata = ImmutableMap.copyOf(secondaryAutomata);
        this.fastTrap = optimisations.contains(Optimisation.FAST_TRAP);
    }

    public GenericProduct(Automaton<P> primaryAutomaton, Collection<K> keys, Function<K, ? extends Automaton<S>> constructor, ValuationSetFactory<String> valuationSetFactory, Collection<Optimisation> optimisations) {
        super(valuationSetFactory);
        this.primaryAutomaton = primaryAutomaton;

        ImmutableMap.Builder<K, Automaton<S>> builder = ImmutableMap.builder();
        keys.forEach(k -> builder.put(k, constructor.apply(k)));
        this.secondaryAutomata = builder.build();

        this.fastTrap = optimisations.contains(Optimisation.FAST_TRAP);
    }

    protected abstract Set<K> relevantSecondary(P primaryState);

    public class GenericProductState implements IState<GenericProductState> {

        protected final P primaryState;
        protected final Map<K, S> secondaryStates;

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

        // TODO: Drop getters?
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
            GenericProductState that = (GenericProductState) o;
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

        @Override
        public GenericProductState getSuccessor(Set<String> valuation) {
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
                    } else if (fastTrap) {
                        return null;
                    }
                }
            }

            return new GenericProductState(primarySuccessor, builder.build());
        }

        @Override
        public boolean isAccepting(Set<String> valuation) {
            return false;
        }

        @Override
        public Set<ValuationSet> partitionSuccessors() {
            List<Set<ValuationSet>> product = new ArrayList<>(secondaryStates.size() + 1);

            product.add(primaryState.partitionSuccessors());

            for (IState secondaryState : secondaryStates.values()) {
                product.add(secondaryState.partitionSuccessors());
            }

            return Sets.cartesianProduct(product).stream()
                    .map(this::flatten)
                    .filter(set -> !set.isEmpty())
                    .collect(Collectors.toSet());
        }

        private ValuationSet flatten(List<ValuationSet> list) {
            ValuationSet flattSet = valuationSetFactory.createUniverseValuationSet();

            for (ValuationSet set : list) {
                flattSet.retainAll(set);
            }

            return flattSet;
        }
    }
}
