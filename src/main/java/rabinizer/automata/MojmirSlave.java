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

import ltl.UnaryModalOperator;
import omega_automaton.Automaton;
import omega_automaton.AutomatonState;
import omega_automaton.Edge;
import omega_automaton.acceptance.AllAcceptance;
import omega_automaton.collections.TranSet;
import omega_automaton.collections.valuationset.ValuationSet;
import omega_automaton.collections.valuationset.ValuationSetFactory;
import rabinizer.frequencyLTL.UnfoldNoSlaveOperatorVisitor;
import ltl.equivalence.EquivalenceClass;
import ltl.equivalence.EquivalenceClassFactory;

import java.util.BitSet;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

public class MojmirSlave extends Automaton<MojmirSlave.State, AllAcceptance> {

    final UnaryModalOperator label;
    private final boolean eager;
    private final EquivalenceClass initialStateEquivalence;

    public MojmirSlave(UnaryModalOperator formula, EquivalenceClassFactory equivalenceClassFactory, ValuationSetFactory valuationSetFactory, Collection<Optimisation> optimisations) {
        super(valuationSetFactory);
        initialStateEquivalence = equivalenceClassFactory.createEquivalenceClass(formula.operand);
        eager = optimisations.contains(Optimisation.EAGER);
        label = formula;

    }

    @Override
    protected State generateInitialState() {
        if (eager) {
            return new State(initialStateEquivalence.apply(formula -> formula.accept(new UnfoldNoSlaveOperatorVisitor())));
        } else {
            return new State(initialStateEquivalence);
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

    public final class State extends AbstractFormulaState implements AutomatonState<State> {
        State(EquivalenceClass clazz) {
            super(clazz);
        }

        @Override
        public Edge<State> getSuccessor(BitSet valuation) {
            if (eager) {
                return new Edge<>(new State(clazz.temporalStep(valuation).apply(formula -> formula.accept(new UnfoldNoSlaveOperatorVisitor()))), new BitSet(0));
            } else {
                return new Edge<>(new State(clazz.apply(formula -> formula.accept(new UnfoldNoSlaveOperatorVisitor())).temporalStep(valuation)), new BitSet(0));
            }
        }

        @Override
        public BitSet getSensitiveAlphabet() {
            return getSensitive(false);
        }

        @Override
        protected Object getOuter() {
            return MojmirSlave.this;
        }

        ValuationSet getFailingMojmirTransitions(Set<MojmirSlave.State> finalStates) {
            ValuationSet fail = valuationSetFactory.createEmptyValuationSet();
            if (finalStates.contains(this)) {
                return fail;
            }
            for (Entry<Edge<MojmirSlave.State>, ValuationSet> valuationSetFailState : MojmirSlave.this.getSuccessors(this).entrySet()) {
                if (isSink(valuationSetFailState.getKey().successor) && !finalStates.contains(valuationSetFailState.getKey().successor)) {
                    fail.addAll(valuationSetFailState.getValue());
                }
            }
            return fail;
        }

        ValuationSet getSucceedMojmirTransitions(Set<MojmirSlave.State> finalStates) {
            ValuationSet succeed = valuationSetFactory.createEmptyValuationSet();
            if (!finalStates.contains(this)) {
                for (Entry<Edge<MojmirSlave.State>, ValuationSet> valuation : MojmirSlave.this.getSuccessors(this).entrySet()) {
                    if (finalStates.contains(valuation.getKey().successor)) {
                        succeed.addAll(valuation.getValue());
                    }
                }
            } else if (finalStates.contains(getInitialState())) {
                succeed.addAll(valuationSetFactory.createUniverseValuationSet());
            }
            return succeed;
        }

        @Override
        public ValuationSetFactory getFactory() {
            return valuationSetFactory;
        }
    }
}
