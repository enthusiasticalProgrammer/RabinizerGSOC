package rabinizer.automata.nxt;

import rabinizer.automata.Automaton;
import rabinizer.automata.IState;
import rabinizer.automata.Optimisation;
import rabinizer.ltl.*;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

public class DetLimitSlave extends Automaton<DetLimitSlave.State> {

    private final EquivalenceClass initialFormula;
    private final EquivalenceClass True;
    private final boolean eager;
    private final boolean cover;

    public DetLimitSlave(Formula formula, EquivalenceClassFactory equivalenceClassFactory, ValuationSetFactory<String> valuationSetFactory, Collection<Optimisation> optimisations) {
        super(valuationSetFactory);
        eager = optimisations.contains(Optimisation.EAGER);
        cover = optimisations.contains(Optimisation.COVER);
        initialFormula = eager ? equivalenceClassFactory.createEquivalenceClass(formula.unfold(true)) : equivalenceClassFactory.createEquivalenceClass(formula);
        True = equivalenceClassFactory.getTrue();
    }

    @Override
    protected State generateInitialState() {
        return new State(initialFormula, True);
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
                    Objects.equals(next, that.next);
        }

        @Override
        public int hashCode() {
            return Objects.hash(current, next);
        }

        @Override
        public String toString() {
            return "{" + Simplifier.simplify(current.getRepresentative(), Simplifier.Strategy.PROPOSITIONAL) + ", "
                    + Simplifier.simplify(next.getRepresentative(), Simplifier.Strategy.PROPOSITIONAL) + "}";
        }

        @Override
        public State getSuccessor(Set<String> valuation) {
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

        @Override
        public boolean isAccepting(Set<String> valuation) {
            EquivalenceClass successor = step(current, valuation);
            return successor.isTrue();
        }

        @Override
        public Set<ValuationSet> partitionSuccessors() {
            return valuationSetFactory.createAllValuationSets();
        }

        private State removeCover(EquivalenceClass currentSuccessor, EquivalenceClass nextCandidate) {
            EquivalenceClass nextSuccessor;

            if (!currentSuccessor.implies(nextCandidate)) {
                nextSuccessor = nextCandidate;
            } else {
                nextSuccessor = True;
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
