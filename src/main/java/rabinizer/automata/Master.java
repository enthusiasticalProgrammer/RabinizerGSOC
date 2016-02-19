/*
 * Copyright (C) 2016  (See AUTHORS)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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

    public Master(@Nullable EquivalenceClass clazz, ValuationSetFactory valuationSetFactory, Collection<Optimisation> optimisations, boolean mergingEnabled) {
        super(valuationSetFactory, mergingEnabled);
        initialState = clazz;
        eager = optimisations.contains(Optimisation.EAGER);
    }

    public Master(ValuationSetFactory valuationSetFactory, Collection<Optimisation> optimisations) {
        this(null, valuationSetFactory, optimisations, true);
    }

    public Master(@NotNull Formula formula, EquivalenceClassFactory equivalenceClassFactory,
            ValuationSetFactory valuationSetFactory, Collection<Optimisation> optimisations, boolean mergingEnabled) {
        this(equivalenceClassFactory.createEquivalenceClass(formula), valuationSetFactory, optimisations, mergingEnabled);
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
        public @NotNull Set<ValuationSet> partitionSuccessors() {
            if (eager) {
                return generatePartitioning(clazz.getRepresentative());
            } else {
                return generatePartitioning(clazz.unfold(true).getRepresentative());
            }
        }

        @Override
        public @NotNull Set<String> getSensitiveAlphabet() {
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
        public @NotNull ValuationSetFactory getFactory() {
            return valuationSetFactory;
        }
    }
}
