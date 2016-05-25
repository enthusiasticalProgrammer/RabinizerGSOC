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

import rabinizer.collections.valuationset.ValuationSet;
import rabinizer.collections.valuationset.ValuationSetFactory;
import ltl.Formula;
import ltl.equivalence.EquivalenceClass;
import ltl.equivalence.EquivalenceClassFactory;

import javax.annotation.Nullable;
import java.util.BitSet;
import java.util.Collection;

public class Master extends Automaton<Master.State> {

    final boolean eager;
    @Nullable
    final EquivalenceClass initialClazz;

    public Master(@Nullable EquivalenceClass clazz, ValuationSetFactory valuationSetFactory, Collection<Optimisation> optimisations) {
        super(valuationSetFactory);
        initialClazz = clazz;
        eager = optimisations.contains(Optimisation.EAGER);
    }

    public Master(ValuationSetFactory valuationSetFactory, Collection<Optimisation> optimisations) {
        this(null, valuationSetFactory, optimisations);
    }

    public Master(Formula formula, EquivalenceClassFactory equivalenceClassFactory,
                  ValuationSetFactory valuationSetFactory, Collection<Optimisation> optimisations) {
        this(equivalenceClassFactory.createEquivalenceClass(formula), valuationSetFactory, optimisations);
    }

    public State generateInitialState(EquivalenceClass clazz) {
        if (eager) {
            return new State(clazz.unfold(true));
        } else {
            return new State(clazz);
        }
    }

    @Override
    protected State generateInitialState() {
        if (initialClazz == null) {
            throw new IllegalStateException("There is no initial state!");
        }

        return generateInitialState(initialClazz);
    }

    protected EquivalenceClass step(EquivalenceClass clazz, BitSet valuation) {
        if (eager) {
            return clazz.temporalStep(valuation).unfold(true);
        } else {
            return clazz.unfold(true).temporalStep(valuation);
        }
    }

    protected boolean suppressEdge(EquivalenceClass current, EquivalenceClass successor) {
        return successor.isFalse();
    }

    public class State extends AbstractFormulaState implements IState<State> {

        public State(EquivalenceClass clazz) {
            super(clazz);
        }

        @Nullable
        @Override
        public State getSuccessor(BitSet valuation) {
            EquivalenceClass successor = step(clazz, valuation);

            if (suppressEdge(clazz, successor)) {
                return null;
            }

            return new State(successor);
        }

        @Override
        public BitSet getSensitiveAlphabet() {
            return getSensitive(true);
        }

        @Override
        public ValuationSetFactory getFactory() {
            return valuationSetFactory;
        }

        @Override
        protected Object getOuter() {
            return Master.this;
        }
    }
}
