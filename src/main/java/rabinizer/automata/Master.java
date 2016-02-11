package rabinizer.automata;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rabinizer.ltl.equivalence.EquivalenceClass;
import rabinizer.ltl.equivalence.EquivalenceClassFactory;
import rabinizer.ltl.*;
import rabinizer.collections.valuationset.ValuationSet;
import rabinizer.collections.valuationset.ValuationSetFactory;

import java.util.Collection;
import java.util.Set;

public class Master extends Automaton<Master.State> {

    final boolean eager;
    final @Nullable EquivalenceClass initialState;
    final EquivalenceClass TRUE;

    public Master(EquivalenceClass clazz, EquivalenceClassFactory equivalenceClassFactory, ValuationSetFactory valuationSetFactory, Collection<Optimisation> optimisations, boolean mergingEnabled) {
        super(valuationSetFactory, mergingEnabled);
        initialState = clazz;
        eager = optimisations.contains(Optimisation.EAGER);
        TRUE = equivalenceClassFactory.getTrue();
    }

    public Master(EquivalenceClassFactory equivalenceClassFactory, ValuationSetFactory valuationSetFactory, Collection<Optimisation> optimisations, boolean mergingEnabled) {
        this((EquivalenceClass) null, equivalenceClassFactory, valuationSetFactory, optimisations, mergingEnabled);
    }

    public Master(Formula formula, EquivalenceClassFactory equivalenceClassFactory,
                  ValuationSetFactory valuationSetFactory, Collection<Optimisation> optimisations, boolean mergingEnabled) {
        this(equivalenceClassFactory.createEquivalenceClass(formula), equivalenceClassFactory, valuationSetFactory, optimisations, mergingEnabled);
    }

    @Override
    protected @NotNull State generateInitialState() {
        if (initialState == null) {
            throw new IllegalStateException("There is no initial state!");
        }

        return generateInitialState(initialState);
    }

    public State generateInitialState(@NotNull EquivalenceClass clazz) {
        if (eager) {
            return new State(clazz.unfold(true));
        } else {
            return new State(clazz);
        }
    }

    protected EquivalenceClass step(EquivalenceClass clazz, Set<String> valuation) {
        if (eager) {
            return clazz.temporalStep(valuation).unfold(true);
        } else {
            return clazz.unfold(true).temporalStep(valuation);
        }
    }

    protected boolean suppressEdge(EquivalenceClass current, Set<String> valuation, EquivalenceClass successor) {
        return successor.isFalse();
    }

    public class State extends AbstractFormulaState implements IState<State> {

        public State(EquivalenceClass clazz) {
            super(clazz);
        }

        @Override
        public @Nullable State getSuccessor(@NotNull Set<String> valuation) {
            EquivalenceClass successor = step(clazz, valuation);

            if (suppressEdge(clazz, valuation, successor)) {
                return null;
            }

            return new State(successor);
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
        public Set<String> getSensitiveAlphabet() {
            return getSensitive(true);
        }

        @Override
        protected Object getOuter() {
            return Master.this;
        }

        @Override
        protected ValuationSet createUniverseValuationSet() {
            return valuationSetFactory.createUniverseValuationSet();
        }

        @Override
        public ValuationSetFactory getFactory() {
            return valuationSetFactory;
        }
    }
}
