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
import ltl.GOperator;
import ltl.equivalence.EquivalenceClass;
import ltl.equivalence.EquivalenceClassFactory;

import java.util.BitSet;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MojmirSlave extends Automaton<MojmirSlave.State> {

    protected final GOperator label;
    private final boolean eager;
    private final EquivalenceClass initialState;

    public MojmirSlave(GOperator formula, EquivalenceClassFactory equivalenceClassFactory,
                       ValuationSetFactory valuationSetFactory, Collection<Optimisation> optimisations) {
        super(valuationSetFactory);
        initialState = equivalenceClassFactory.createEquivalenceClass(formula.operand);
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

    public TranSet<State> getAllTransitions() {
        TranSet<State> result = new TranSet<>(valuationSetFactory);
        this.getStates().forEach(state -> result.addAll(state, valuationSetFactory.createUniverseValuationSet()));
        return result;
    }

    protected Collection<State> getSinks() {
        return getStates().stream().filter(this::isSink).collect(Collectors.toList());
    }

    public final class State extends AbstractFormulaState implements IState<State> {
        State(EquivalenceClass clazz) {
            super(clazz);
        }

        @Override
        public State getSuccessor(BitSet valuation) {
            if (eager) {
                return new State(clazz.temporalStep(valuation).unfold(false));
            } else {
                return new State(clazz.unfold(false).temporalStep(valuation));
            }
        }

        @Override
        public BitSet getSensitiveAlphabet() {
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

        ValuationSet getFailingMojmirTransitions(Set<State> finalStates) {
            ValuationSet fail = valuationSetFactory.createEmptyValuationSet();
            if (finalStates.contains(this)) {
                return fail;
            }
            for (Map.Entry<MojmirSlave.State, ValuationSet> valuationSetFailState : this.getSuccessors().entrySet()) {
                if (isSink(valuationSetFailState.getKey()) && !finalStates.contains(valuationSetFailState.getKey())) {
                    fail.addAll(valuationSetFailState.getValue());
                }
            }
            return fail;
        }

        ValuationSet getSucceedMojmirTransitions(Set<State> finalStates) {
            ValuationSet succeed = valuationSetFactory.createEmptyValuationSet();
            if (!finalStates.contains(this)) {
                for (Map.Entry<MojmirSlave.State, ValuationSet> valuation : getSuccessors().entrySet()) {
                    if (finalStates.contains(valuation.getKey())) {
                        succeed.addAll(valuation.getValue());
                    }
                }
            } else if (finalStates.contains(getInitialState())) {
                succeed.addAll(valuationSetFactory.createUniverseValuationSet());
            }
            return succeed;
        }
    }
}
