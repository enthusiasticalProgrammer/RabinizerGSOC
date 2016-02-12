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
import rabinizer.ltl.equivalence.EquivalenceClass;
import rabinizer.ltl.equivalence.EquivalenceClassFactory;
import rabinizer.ltl.*;
import rabinizer.collections.valuationset.ValuationSet;
import rabinizer.collections.valuationset.ValuationSetFactory;

import java.util.Collection;
import java.util.Set;

public class MojmirSlave extends Automaton<MojmirSlave.State> {

    public final GOperator label;
    final boolean eager;
    final EquivalenceClass initialState;

    public MojmirSlave(GOperator formula, EquivalenceClassFactory equivalenceClassFactory,
                       ValuationSetFactory valuationSetFactory, Collection<Optimisation> optimisations) {
        super(valuationSetFactory);
        initialState = equivalenceClassFactory.createEquivalenceClass(formula.operand);
        eager = optimisations.contains(Optimisation.EAGER);
        label = formula;
    }

    @Override
    protected @NotNull State generateInitialState() {
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
        public State getSuccessor(@NotNull Set<String> valuation) {
            if (eager) {
                return new State(clazz.temporalStep(valuation).unfold(false));
            } else {
                return new State(clazz.unfold(false).temporalStep(valuation));
            }
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
        public Set<String> getSensitiveAlphabet() {
            return getSensitive(false);
        }

        @Override
        public ValuationSetFactory getFactory() {
            return valuationSetFactory;
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
