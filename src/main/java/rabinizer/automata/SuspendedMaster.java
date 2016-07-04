package rabinizer.automata;


import omega_automaton.AutomatonState;
import omega_automaton.Edge;
import omega_automaton.collections.valuationset.ValuationSet;
import omega_automaton.collections.valuationset.ValuationSetFactory;
import rabinizer.frequencyLTL.UnfoldNoSlaveOperatorVisitor;
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
            return new State(clazz.unfold(), suspendable, clazz.apply(formula -> formula.accept(new UnfoldNoSlaveOperatorVisitor())));
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
            return clazz.temporalStep(valuation).apply(formula -> formula.accept(new UnfoldNoSlaveOperatorVisitor()));
        } else {
            return clazz.apply(formula -> formula.accept(new UnfoldNoSlaveOperatorVisitor())).temporalStep(valuation);
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

    /**
     * The method replaces antecessor by replacement. Both must be in the
     * states-set (and both must not be null) when calling the method.
     * Antecessor gets deleted during the method, and the transitions to
     * antecessor will be recurved towards replacement.
     * <p>
     * The method throws an IllegalArgumentException, when one of the parameters
     * is not in the states-set
     */
    protected void replaceBy(State antecessor, State replacement) {
        if (!(transitions.containsKey(antecessor) && transitions.containsKey(replacement))) {
            throw new IllegalArgumentException();
        }

        transitions.remove(antecessor).clear();

        for (Map<Edge<Master.State>, ValuationSet> edges : transitions.values()) {
            ValuationSet vs = edges.get(new Edge<>(antecessor, new BitSet(0)));

            if (vs == null) {
                continue;
            }

            ValuationSet vs2 = edges.get(replacement);

            if (vs2 == null) {
                edges.put(new Edge<>(replacement, new BitSet(0)), vs);
            } else {
                vs2.addAll(vs);
            }
        }

        if (antecessor.equals(initialState)) {
            initialState = replacement;
        }
    }

    public class State extends Master.State implements AutomatonState<Master.State> {

        final boolean slavesSuspended;
        private final EquivalenceClass folded;

        public State(EquivalenceClass clazz, boolean slavesSuspended, EquivalenceClass folded) {
            super(clazz);
            this.slavesSuspended = slavesSuspended;
            this.folded = folded;
        }

        @Nullable
        @Override
        public Edge<rabinizer.automata.Master.State> getSuccessor(BitSet valuation) {
            EquivalenceClass successor = step(clazz, valuation);
            EquivalenceClass folded = stepTest(this.folded, valuation);

            if (suppressEdge(clazz, successor)) {
                return null;
            }

            if (slaveSuspension && this.slavesSuspended && !folded.getRepresentative().accept(RelevantGFormulaeWithSlaveSuspension.RELEVANT_G_FORMULAE_PRESENT)) {
                return new Edge<>(new State(successor, true, folded), new BitSet(0));
            }

            return new Edge<>(new State(successor, false, folded), new BitSet(0));
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
