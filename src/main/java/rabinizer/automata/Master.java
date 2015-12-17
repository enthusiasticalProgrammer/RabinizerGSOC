package rabinizer.automata;

import rabinizer.ltl.*;

import java.util.Collection;
import java.util.Set;

public class Master extends Automaton<Master.State> {

    private final boolean eager;
    private final EquivalenceClass initialState;

    public Master(Formula formula, EquivalenceClassFactory equivalenceClassFactory,
                  ValuationSetFactory<String> valuationSetFactory, Collection<Optimisation> optimisations) {
        super(valuationSetFactory);
        initialState = equivalenceClassFactory.createEquivalenceClass(formula);
        eager = optimisations.contains(Optimisation.EAGER);
    }

    @Override
    protected State generateInitialState() {
        if (eager) {
            return new State(initialState.unfold(true));
        } else {
            return new State(initialState);
        }
    }

    public class State extends AbstractFormulaState implements IState<State> {

        State(EquivalenceClass clazz) {
            super(clazz);
        }

        @Override
        public State getSuccessor(Set<String> valuation) {
            EquivalenceClass successor;

            if (eager) {
                successor = clazz.temporalStep(valuation).unfold(true);
            } else {
                successor = clazz.unfold(true).temporalStep(valuation);
            }

            if (successor.isFalse()) {
                return null;
            }

            return new State(successor);
        }

        @Override
        public boolean isAccepting(Set<String> valuation) {
            return false;
        }

        @Override
        public Set<ValuationSet> partitionSuccessors() {
            if (eager) {
                return generatePartitioning(clazz.getRepresentative());
            } else {
                return generatePartitioning(clazz.unfold(true).getRepresentative());
            }
        }

        @Override
        protected Object getOuter() {
            return Master.this;
        }

        @Override
        protected ValuationSet createUniverseValuationSet() {
            return valuationSetFactory.createUniverseValuationSet();
        }
    }
}
