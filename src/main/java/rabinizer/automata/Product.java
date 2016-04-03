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
import rabinizer.collections.valuationset.ValuationSetFactory;
import rabinizer.ltl.GOperator;

import java.util.*;
import java.util.function.Function;

public class Product extends Automaton<Product.ProductState> {

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

    public class ProductState extends AbstractProductState<Master.State, GOperator, RabinSlave.State, ProductState> implements IState<ProductState> {

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
        protected Automaton<Master.State> getPrimaryAutomaton() {
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
    }
}
