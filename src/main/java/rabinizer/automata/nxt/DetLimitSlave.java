package rabinizer.automata.nxt;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import rabinizer.automata.Automaton;
import rabinizer.automata.IState;
import rabinizer.automata.Optimisation;
import rabinizer.ltl.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class DetLimitSlave extends Automaton<DetLimitSlave.State> {

    protected final EquivalenceClass initialFormula;
    protected final EquivalenceClass True;
    protected final boolean eager;
    protected final boolean cover;

    private final LoadingCache<State, ValuationSet> acceptanceCache;

    public DetLimitSlave(Formula formula, EquivalenceClassFactory equivalenceClassFactory, ValuationSetFactory valuationSetFactory, Collection<Optimisation> optimisations) {
        super(valuationSetFactory, false);
        eager = optimisations.contains(Optimisation.EAGER);
        cover = optimisations.contains(Optimisation.COVER);
        initialFormula = eager ? equivalenceClassFactory.createEquivalenceClass(formula.unfold(true)) : equivalenceClassFactory.createEquivalenceClass(formula);
        True = equivalenceClassFactory.getTrue();

        CacheLoader<State, ValuationSet> acceptanceLoader = new CacheLoader<State, ValuationSet>() {
            @Override
            public ValuationSet load(State arg) {
                return arg.getAcceptance();
            }
        };

        acceptanceCache = CacheBuilder.newBuilder().build(acceptanceLoader);
    }

    @Override
    protected @NotNull State generateInitialState() {
        return new State(initialFormula, True);
    }

    public ValuationSet getAcceptance(State state) {
        return acceptanceCache.getUnchecked(state);
    }

    public final class State implements IState<State> {

        final EquivalenceClass current;
        final EquivalenceClass next;

        State(EquivalenceClass current, EquivalenceClass next) {
            this.current = current;
            this.next = next;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            State that = (State) o;
            return Objects.equals(current, that.current) &&
                    Objects.equals(next, that.next) &&
                    Objects.equals(initialFormula, that.getInitialFormula());
        }

        @Override
        public int hashCode() {
            return Objects.hash(current, next, initialFormula);
        }

        @Override
        public String toString() {
            return "{" + current.getRepresentative() + ", " + next.getRepresentative() + '}';
        }

        @Override
        public State getSuccessor(@NotNull Set<String> valuation) {
            EquivalenceClass successor = step(current, valuation);

            // Successor is done and we can switch components.
            if (successor.isTrue()) {
                return new State(step(next, valuation).and(initialFormula), True);
            }

            // We cannot recover from false. (non-accepting trap)
            if (successor.isFalse()) {
                return null;
            }

            EquivalenceClass nextSuccessor = step(next, valuation);

            if (cover) {
                return removeCover(successor, nextSuccessor);
            }

            return new State(successor, nextSuccessor.and(initialFormula));
        }

        public ValuationSet getAcceptance() {
            Set<String> sensitiveLetters = new HashSet<>();

            for (Formula literal : current.unfold(true).getSupport()) {
                if (literal instanceof Literal) {
                    sensitiveLetters.add(((Literal) literal).getAtom());
                }
            }

            ValuationSet acceptingLetters = valuationSetFactory.createEmptyValuationSet();

            for (Set<String> valuation : Sets.powerSet(sensitiveLetters)) {
                EquivalenceClass successor = step(current, valuation);
                if (successor.isTrue()) {
                    acceptingLetters.addAll(valuationSetFactory.createValuationSet(valuation, sensitiveLetters));
                }
            }

            return acceptingLetters;
        }

        @Override
        public Set<String> getSensitiveAlphabet() {
            Set<String> sensitiveLetters = new HashSet<>();

            for (Formula literal : Sets.union(current.unfold(true).getSupport(), next.unfold(true).getSupport())) {
                if (literal instanceof Literal) {
                    sensitiveLetters.add(((Literal) literal).getAtom());
                }
            }

            return sensitiveLetters;
        }

        @Override
        public ValuationSetFactory getFactory() {
            return valuationSetFactory;
        }

        private EquivalenceClass getInitialFormula() {
            return initialFormula;
        }

        private State removeCover(EquivalenceClass currentSuccessor, EquivalenceClass nextCandidate) {
            EquivalenceClass nextSuccessor;

            if (currentSuccessor.implies(nextCandidate)) {
                nextSuccessor = True;
            } else {
                nextSuccessor = nextCandidate;
            }

            if (!currentSuccessor.implies(initialFormula)) {
                nextSuccessor = nextSuccessor.and(initialFormula);
            }

            return new State(currentSuccessor, nextSuccessor);
        }

        private EquivalenceClass step(EquivalenceClass clazz, Set<String> valuation) {
            if (eager) {
                return clazz.temporalStep(valuation).unfold(true);
            } else {
                return clazz.unfold(true).temporalStep(valuation);
            }
        }
    }
}
