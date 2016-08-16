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

import ltl.Formula;
import ltl.equivalence.EquivalenceClass;
import ltl.equivalence.EquivalenceClassFactory;
import omega_automaton.Automaton;
import omega_automaton.AutomatonState;
import omega_automaton.Edge;
import omega_automaton.acceptance.AllAcceptance;
import omega_automaton.collections.valuationset.ValuationSetFactory;

import javax.annotation.Nullable;
import java.util.BitSet;
import java.util.Collection;

class Master extends Automaton<Master.State, AllAcceptance> {

    final boolean eager;
    @Nullable
    final EquivalenceClass initialClazz;

    Master(@Nullable EquivalenceClass clazz, ValuationSetFactory valuationSetFactory, Collection<Optimisation> optimisations) {
        super(null, valuationSetFactory);
        initialClazz = clazz;
        eager = optimisations.contains(Optimisation.EAGER);
    }

    Master(Formula formula, EquivalenceClassFactory equivalenceClassFactory,
                  ValuationSetFactory valuationSetFactory, Collection<Optimisation> optimisations) {
        this(equivalenceClassFactory.createEquivalenceClass(formula), valuationSetFactory, optimisations);
    }

    public State generateInitialState(EquivalenceClass clazz) {
        if (eager) {
            return new State(clazz.unfold());
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
            return clazz.temporalStep(valuation).unfold();
        } else {
            return clazz.unfold().temporalStep(valuation);
        }
    }

    public class State extends AbstractFormulaState implements AutomatonState<State> {

        public State(EquivalenceClass clazz) {
            super(clazz);
        }

        @Nullable
        @Override
        public Edge<State> getSuccessor(BitSet valuation) {
            EquivalenceClass successor = step(clazz, valuation);

            if (successor.isFalse()) {
                return null;
            }

            return new Edge<>(new State(successor), new BitSet(0));
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
