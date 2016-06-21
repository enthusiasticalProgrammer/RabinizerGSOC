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

import com.google.common.collect.ImmutableMap;

import rabinizer.automata.MojmirSlave.State;
import rabinizer.automata.Product.ProductState;
import omega_automaton.Automaton;
import omega_automaton.AutomatonState;
import omega_automaton.acceptance.GeneralisedRabinAcceptance;
import omega_automaton.collections.TranSet;
import omega_automaton.collections.Tuple;
import omega_automaton.collections.valuationset.ValuationSet;
import omega_automaton.collections.valuationset.ValuationSetFactory;
import ltl.GOperator;

import java.util.*;
import java.util.function.Function;

public class Product extends Automaton<Product.ProductState, GeneralisedRabinAcceptance<ProductState>> {

    protected final Master primaryAutomaton;
    protected final Map<GOperator, RabinSlave> secondaryAutomata;

    protected final boolean allSlaves;

    public Product(Master primaryAutomaton, Map<GOperator, RabinSlave> slaves, ValuationSetFactory factory, Collection<Optimisation> optimisations) {
        super(factory);
        // relevant secondaryAutomata dynamically
        // computed from primaryAutomaton formula
        // master formula
        this.primaryAutomaton = primaryAutomaton;
        this.secondaryAutomata = slaves;
        this.allSlaves = !optimisations.contains(Optimisation.ONLY_RELEVANT_SLAVES);
    }

    @Override
    protected Product.ProductState generateInitialState() {
        return new ProductState(primaryAutomaton.getInitialState(), relevantSecondarySlaves(primaryAutomaton.getInitialState()),
                k -> secondaryAutomata.get(k).getInitialState());
    }

    private Set<GOperator> relevantSecondarySlaves(Master.State primaryState) {
        Set<GOperator> keys;
        if (allSlaves) {
            keys = secondaryAutomata.keySet();
        } else {
            keys = new HashSet<>();
            primaryState.getClazz().getSupport().forEach(f -> keys.addAll(f.gSubformulas()));
        }

        if (primaryState instanceof SuspendedMaster.State && ((SuspendedMaster.State) primaryState).slavesSuspended) {
            return Collections.emptySet();
        }

        return keys;
    }

    protected TranSet<ProductState> getFailingProductTransitions(RabinSlave slave, Set<MojmirSlave.State> finalStates) {
        TranSet<Product.ProductState> failP = new TranSet<>(valuationSetFactory);
        for (ProductState ps : getStates()) {
            failP.addAll(ps.getFailTransitions(slave.mojmir, finalStates));

        }

        return failP;
    }

    protected TranSet<Product.ProductState> getSucceedingProductTransitions(RabinSlave slave, int rank, Set<MojmirSlave.State> finalStates) {
        TranSet<Product.ProductState> succeedP = new TranSet<>(valuationSetFactory);
        for (Product.ProductState ps : getStates()) {
            succeedP.addAll(ps, ps.getSucceedTransitions(slave.mojmir, rank, finalStates));
        }
        return succeedP;
    }

    protected TranSet<Product.ProductState> getBuyProductTransitions(RabinSlave slave, Set<MojmirSlave.State> finalStates, int rank) {
        TranSet<Product.ProductState> buyP = new TranSet<>(valuationSetFactory);
        for (Product.ProductState ps : getStates()) {
            RabinSlave.State rs = ps.secondaryStates.get(slave.mojmir.label);
            if (rs != null) { // relevant slave
                buyP.addAll(ps, ps.getBuyTransitions(slave.mojmir, rank, finalStates));
            }
        }

        return buyP;
    }

    Tuple<TranSet<Product.ProductState>, TranSet<Product.ProductState>> createRabinPair(RabinSlave slave, Set<State> finalStates, int rank) {
        TranSet<ProductState> failP = getFailingProductTransitions(slave, finalStates);
        TranSet<ProductState> succeedP = getSucceedingProductTransitions(slave, rank, finalStates);
        TranSet<ProductState> buyP = getBuyProductTransitions(slave, finalStates, rank);
        failP.addAll(buyP);
        return new Tuple<>(failP, succeedP);
    }

    public class ProductState extends AbstractProductState<Master.State, GOperator, RabinSlave.State, ProductState> implements AutomatonState<ProductState> {

        private ProductState(Master.State primaryState, ImmutableMap<GOperator, RabinSlave.State> secondaryStates) {
            super(primaryState, secondaryStates);
        }

        private ProductState(Master.State primaryState, Collection<GOperator> keys, Function<GOperator, RabinSlave.State> constructor) {
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
        protected Map<GOperator, RabinSlave> getSecondaryAutomata() {
            return secondaryAutomata;
        }

        @Override
        protected Set<GOperator> relevantSecondary(Master.State primaryState) {
            return relevantSecondarySlaves(primaryState);
        }

        @Override
        protected ProductState constructState(Master.State primaryState, ImmutableMap<GOperator, RabinSlave.State> secondaryStates) {
            return new ProductState(primaryState, secondaryStates);
        }

        @Override
        protected Iterable<Tuple<Map<GOperator, RabinSlave.State>, ValuationSet>> secondaryJointMove(Set<GOperator> keys, ValuationSet maxVs) {
            ArrayDeque<Tuple<Map<GOperator, RabinSlave.State>, ValuationSet>> result = new ArrayDeque<>();
            if (this.primaryState instanceof SuspendedMaster.State) {
                SuspendedMaster.State mine = (SuspendedMaster.State) this.primaryState;
                if (mine.slavesSuspended) {
                    Map<Master.State, ValuationSet> primarySuccessors = getPrimaryAutomaton().getSuccessors(primaryState);

                    for (Map.Entry<Master.State, ValuationSet> entry1 : primarySuccessors.entrySet()) {
                        Map<GOperator, RabinSlave.State> map = new HashMap<>();
                        if (!((SuspendedMaster.State) entry1.getKey()).slavesSuspended) {
                            for (GOperator g : relevantSecondary(entry1.getKey())) {
                                map.put(g, secondaryAutomata.get(g).getInitialState());
                            }
                        }
                        ValuationSet valu = entry1.getValue().intersect(maxVs);
                        if (!valu.isEmpty()) {
                            result.add(new Tuple<>(map, valu));
                        }
                    }
                    return result;
                }
            }

            return super.secondaryJointMove(keys, maxVs);
        }

        private TranSet<ProductState> getFailTransitions(MojmirSlave mojmir, Set<MojmirSlave.State> finalStates) {
            TranSet<ProductState> fail = new TranSet<>(valuationSetFactory);
            RabinSlave.State rs = secondaryStates.get(mojmir.label);
            if (rs != null) { // relevant slave
                for (MojmirSlave.State fs : rs.keySet()) {
                    fail.addAll(this, fs.getFailingMojmirTransitions(finalStates));
                }
            }
            return fail;
        }

        private ValuationSet getSucceedTransitions(MojmirSlave mojmir, int rank, Set<MojmirSlave.State> finalStates) {
            ValuationSet succeed = valuationSetFactory.createEmptyValuationSet();
            RabinSlave.State rs = secondaryStates.get(mojmir.label);
            if (rs != null) { // relevant slave
                for (Map.Entry<MojmirSlave.State, Integer> stateIntegerEntry : rs.entrySet()) {
                    if (stateIntegerEntry.getValue() == rank) {
                        succeed.addAll(stateIntegerEntry.getKey().getSucceedMojmirTransitions(finalStates));
                    }
                }
            }

            return succeed;
        }

        private ValuationSet getBuyTransitions(MojmirSlave mojmir, int rank, Set<MojmirSlave.State> finalStates) {
            ValuationSet buy = valuationSetFactory.createEmptyValuationSet();
            RabinSlave.State rs = secondaryStates.get(mojmir.label);
            if (rs != null) { // relevant slave
                buy.addAll(rs.getBuyTrans(rank, finalStates));
            }
            return buy;
        }
    }
}
