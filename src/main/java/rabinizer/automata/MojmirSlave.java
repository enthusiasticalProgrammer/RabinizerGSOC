package rabinizer.automata;

import rabinizer.ltl.*;

import java.util.Collection;
import java.util.Set;

public class MojmirSlave extends Automaton<MojmirSlave.State> {

    public final GOperator label;
    private final boolean eager;
    private final EquivalenceClass initialState;

    public MojmirSlave(GOperator formula, EquivalenceClassFactory equivalenceClassFactory,
                       ValuationSetFactory<String> valuationSetFactory, Collection<Optimisation> optimisations) {
        super(valuationSetFactory);
        initialState = equivalenceClassFactory.createEquivalenceClass(formula.getOperand());
        eager = optimisations.contains(Optimisation.EAGER);
        label = formula;
    }

    @Override
    protected State generateInitialState() {
        if (eager) {
            return new State(initialState.unfold(false));
        } else {
            return new State(initialState);
        }
    }

    public final class State extends AbstractFormulaState implements IState<State> {
        State(EquivalenceClass clazz) {
            super(clazz);
        }

        @Override
        public State getSuccessor(Set<String> valuation) {
            if (eager) {
                return new State(clazz.temporalStep(valuation).unfold(false));
            } else {
                return new State(clazz.unfold(false).temporalStep(valuation));
            }
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
                return generatePartitioning(clazz.unfold(false).getRepresentative());
            }
        }

        @Override
        protected Object getOuter() {
            return MojmirSlave.this;
        }

        @Override
        protected ValuationSet createUniverseValuationSet() {
            return valuationSetFactory.createUniverseValuationSet();
        }
    }
}
