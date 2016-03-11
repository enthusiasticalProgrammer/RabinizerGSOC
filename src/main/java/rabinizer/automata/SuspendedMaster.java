package rabinizer.automata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import rabinizer.collections.valuationset.ValuationSetFactory;
import rabinizer.ltl.Formula;
import rabinizer.ltl.RelevantGFormulaeWithSlaveSuspension;
import rabinizer.ltl.equivalence.EquivalenceClass;
import rabinizer.ltl.equivalence.EquivalenceClassFactory;

public class SuspendedMaster extends Master {

    final boolean slaveSuspension;

    public SuspendedMaster(@Nullable EquivalenceClass clazz, ValuationSetFactory valuationSetFactory, Collection<Optimisation> optimisations) {
        super(clazz, valuationSetFactory, optimisations, true);
        slaveSuspension = optimisations.contains(Optimisation.SLAVE_SUSPENSION);
    }

    public SuspendedMaster(@NotNull Formula formula, EquivalenceClassFactory equivalenceClassFactory, ValuationSetFactory valuationSetFactory,
                           Collection<Optimisation> optimisations) {
        this(equivalenceClassFactory.createEquivalenceClass(formula), valuationSetFactory, optimisations);
    }

    @Override
    public State generateInitialState(@NotNull EquivalenceClass clazz) {
        boolean suspendable = slaveSuspension && !clazz.getRepresentative().accept(RelevantGFormulaeWithSlaveSuspension.RELEVANT_G_FORMULAE_PRESENT);
        if (eager) {
            return new State(clazz.unfold(true), suspendable, clazz.unfold(false));
        } else {
            return new State(clazz, suspendable, clazz);
        }
    }

    /**
     * This method is there to test, the output is used by
     * relevantGFormulae-Visitor for testing if the successor can be suspended
     *
     */
    private EquivalenceClass stepTest(EquivalenceClass clazz, Set<String> valuation) {
        if (eager) {
            return clazz.temporalStep(valuation).unfold(false);
        } else {
            return clazz.unfold(false).temporalStep(valuation);
        }
    }

    @Override
    public void generate(Master.State initialState) {
        if (!(initialState instanceof State)) {
            throw new AssertionError();
        }
        super.generate(initialState);
        mergeStates();
    }

    private void mergeStates() {
        List<State> worklist = (List<State>) (List<?>) new ArrayList<>(states);
        List<State> worklist2 = (List<State>) (List<?>) new ArrayList<>(states);

        for (State s : worklist) {
            for (State s2 : worklist2) {
                if (!s.equals(s2) && s.almostEquals(s2) && s.slavesSuspended) {
                    replaceBy(s, s2);
                }
            }
        }
    }

    public class State extends Master.State implements IState<Master.State> {

        final boolean slavesSuspended;
        private final EquivalenceClass folded;

        public State(EquivalenceClass clazz, boolean slavesSuspended, EquivalenceClass folded) {
            super(clazz);
            this.slavesSuspended = slavesSuspended;
            this.folded = folded;
        }

        @Override
        public @Nullable State getSuccessor(@NotNull Set<String> valuation) {
            EquivalenceClass successor = step(clazz, valuation);
            EquivalenceClass folded = stepTest(this.folded, valuation);

            if (suppressEdge(clazz, valuation, successor)) {
                return null;
            }

            if (slaveSuspension && this.slavesSuspended && !folded.getRepresentative().accept(RelevantGFormulaeWithSlaveSuspension.RELEVANT_G_FORMULAE_PRESENT)) {
                return new State(successor, true, folded);
            }
            return new State(successor, false, folded);
        }

        @Override
        protected Object getOuter() {
            return SuspendedMaster.this;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null)
                return false;
            if (o.getClass() != this.getClass())
                return false;
            return super.equals(o) && ((State) o).slavesSuspended == this.slavesSuspended;
        }

        /**
         * This method returns equals apart from slavesSuspended, this is
         * important for Master.mergeStates
         */
        private boolean almostEquals(State s) {
            return super.equals(s);
        }
    }

}
