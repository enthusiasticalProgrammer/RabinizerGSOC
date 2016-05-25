package rabinizer.automata;


import rabinizer.collections.valuationset.ValuationSetFactory;
import ltl.Formula;
import ltl.RelevantGFormulaeWithSlaveSuspension;
import ltl.equivalence.EquivalenceClass;
import ltl.equivalence.EquivalenceClassFactory;

import javax.annotation.Nullable;
import java.util.*;

public class SuspendedMaster extends Master {

    final boolean slaveSuspension;

    public SuspendedMaster(@Nullable EquivalenceClass clazz, ValuationSetFactory valuationSetFactory, Collection<Optimisation> optimisations) {
        super(clazz, valuationSetFactory, optimisations);
        slaveSuspension = optimisations.contains(Optimisation.SLAVE_SUSPENSION);
    }

    public SuspendedMaster(Formula formula, EquivalenceClassFactory equivalenceClassFactory, ValuationSetFactory valuationSetFactory,
                           Collection<Optimisation> optimisations) {
        this(equivalenceClassFactory.createEquivalenceClass(formula), valuationSetFactory, optimisations);
    }

    @Override
    public State generateInitialState(EquivalenceClass clazz) {
        boolean suspendable = slaveSuspension && !clazz.getRepresentative().accept(RelevantGFormulaeWithSlaveSuspension.RELEVANT_G_FORMULAE_PRESENT);
        if (eager) {
            return new State(clazz.unfold(true), suspendable, clazz.unfold(false));
        } else {
            return new State(clazz, suspendable, clazz);
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

    /**
     * This method is there to test, the output is used by
     * relevantGFormulae-Visitor for testing if the successor can be suspended
     */
    private EquivalenceClass stepTest(EquivalenceClass clazz, BitSet valuation) {
        if (eager) {
            return clazz.temporalStep(valuation).unfold(false);
        } else {
            return clazz.unfold(false).temporalStep(valuation);
        }
    }

    private void mergeStates() {
        List<State> worklist = (List<State>) (List<?>) new ArrayList<>(getStates());
        List<State> worklist2 = (List<State>) (List<?>) new ArrayList<>(getStates());

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

        @Nullable
        @Override
        public State getSuccessor(BitSet valuation) {
            EquivalenceClass successor = step(clazz, valuation);
            EquivalenceClass folded = stepTest(this.folded, valuation);

            if (suppressEdge(clazz, successor)) {
                return null;
            }

            if (slaveSuspension && this.slavesSuspended && !folded.getRepresentative().accept(RelevantGFormulaeWithSlaveSuspension.RELEVANT_G_FORMULAE_PRESENT)) {
                return new State(successor, true, folded);
            }

            return new State(successor, false, folded);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null)
                return false;
            if (o.getClass() != this.getClass())
                return false;
            return super.equals(o) && ((State) o).slavesSuspended == this.slavesSuspended;
        }

        @Override
        protected Object getOuter() {
            return SuspendedMaster.this;
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
