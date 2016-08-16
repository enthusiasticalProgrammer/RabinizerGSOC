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

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableMap;

import jhoafparser.consumer.HOAConsumer;
import ltl.UnaryModalOperator;
import rabinizer.automata.MojmirSlave.State;
import rabinizer.frequencyLTL.SlaveSubformulaVisitor;
import omega_automaton.Automaton;
import omega_automaton.AutomatonState;
import omega_automaton.acceptance.GeneralisedRabinAcceptance;
import omega_automaton.collections.TranSet;
import omega_automaton.collections.valuationset.ValuationSet;
import omega_automaton.collections.valuationset.ValuationSetFactory;
import omega_automaton.output.HOAConsumerExtended;
import omega_automaton.output.HOAConsumerGeneralisedRabin;

import java.util.*;
import java.util.function.Function;

public abstract class Product<S extends AbstractSelfProductSlave<S>.State> extends Automaton<Product<S>.ProductState, GeneralisedRabinAcceptance<Product<S>.ProductState>> {

    final Master primaryAutomaton;
    private final boolean allSlaves;

    public Product(Master primaryAutomaton, ValuationSetFactory factory, Collection<Optimisation> optimisations) {
        super(null, factory);
        // relevant secondaryAutomata dynamically
        // computed from primaryAutomaton formula
        // master formula
        this.primaryAutomaton = primaryAutomaton;
        this.allSlaves = !optimisations.contains(Optimisation.ONLY_RELEVANT_SLAVES);
    }

    protected final Set<UnaryModalOperator> relevantSecondarySlaves(Master.State primaryState) {
        Set<UnaryModalOperator> keys;
        if (allSlaves) {
            keys = getKeys();
        } else {
            keys = new HashSet<>();
            primaryState.getClazz().getSupport().forEach(f -> keys.addAll(f.accept(new SlaveSubformulaVisitor())));
        }

        return keys;
    }

    boolean containsAllTransitions(TranSet<ProductState> trans) {
        return transitions.entrySet().stream().allMatch(entry -> entry.getValue().entrySet().stream().allMatch(succ -> trans.containsAll(entry.getKey(), succ.getValue())));
    }

    protected abstract Set<UnaryModalOperator> getKeys();

    protected abstract Map<UnaryModalOperator, ? extends AbstractSelfProductSlave<?>> getSecondaryAutomata();

    protected final TranSet<ProductState> getFailingProductTransitions(AbstractSelfProductSlave<?> slave, Set<MojmirSlave.State> finalStates) {
        TranSet<ProductState> failP = new TranSet<>(valuationSetFactory);
        for (ProductState ps : getStates()) {
            failP.addAll(ps.getFailTransitions(slave.mojmir, finalStates));

        }

        return failP;
    }

    /**
     * @param rank
     *            rank is either semantically a rank or the token-number of the
     *            states rank=-1 means that the rank does not matter (used for
     *            ProductControllerSynthesis, F-slave).
     **/
    protected final TranSet<ProductState> getSucceedingProductTransitions(AbstractSelfProductSlave<?> slave, int rank, Set<MojmirSlave.State> finalStates) {
        TranSet<ProductState> succeedP = new TranSet<>(valuationSetFactory);
        for (ProductState ps : getStates()) {
            succeedP.addAll(ps, ps.getSucceedTransitions(slave.mojmir, rank, finalStates));
        }
        return succeedP;
    }

    @Override
    public final void toHOABody(HOAConsumerExtended hoa) {
        for (ProductState s : getStates()) {
            hoa.addState(s);
            getSuccessors(s).forEach((k, v) -> hoa.addEdge(v, k.successor));
            toHOABodyEdge(s, hoa);
            hoa.stateDone();
        }
    }

    @Override
    public void toHOA(HOAConsumer ho, BiMap<String, Integer> aliases) {
        HOAConsumerExtended hoa = new HOAConsumerGeneralisedRabin<>(ho, valuationSetFactory, aliases, initialState, acceptance, size());
        toHOABody(hoa);
        hoa.done();
    }

    /**
     * This method is important, because currently the acceptance is computed
     * after the product is constructed.
     */
    protected void setAcceptance(GeneralisedRabinAcceptance<ProductState> acc) {
        this.acceptance = acc;
    }

    public abstract class ProductState extends AbstractProductState<Master.State, UnaryModalOperator, S, ProductState> implements AutomatonState<ProductState> {

        protected ProductState(Master.State primaryState, ImmutableMap<UnaryModalOperator, S> secondaryStates) {
            super(primaryState, secondaryStates);
        }

        public abstract ValuationSet getSucceedTransitions(MojmirSlave mojmir, int rank, Set<State> finalStates);

        public TranSet<ProductState> getFailTransitions(MojmirSlave mojmir, Set<State> finalStates) {
            TranSet<ProductState> fail = new TranSet<>(valuationSetFactory);
            S rs = secondaryStates.get(mojmir.label);
            if (rs != null) { // relevant slave
                for (MojmirSlave.State fs : rs.keySet()) {
                    fail.addAll(this, fs.getFailingMojmirTransitions(finalStates));
                }
            }
            return fail;
        }

        protected ProductState(Master.State primaryState, Collection<UnaryModalOperator> keys, Function<UnaryModalOperator, S> constructor) {
            super(primaryState, keys, constructor);
        }

        @Override
        public ValuationSetFactory getFactory() {
            return valuationSetFactory;
        }

        @Override
        protected Automaton<Master.State, ?> getPrimaryAutomaton() {
            return primaryAutomaton;
        }

        @Override
        protected Set<UnaryModalOperator> relevantSecondary(Master.State primaryState) {
            return relevantSecondarySlaves(primaryState);
        }

        S getSecondaryState(UnaryModalOperator key) {
            return this.secondaryStates.get(key);
        }
    }
}
