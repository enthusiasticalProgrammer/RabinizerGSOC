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
import rabinizer.collections.valuationset.ValuationSet;
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
        this.trapState = new ProductState(primaryAutomaton.trapState, Collections.emptyMap());
    }

    Set<ValuationSet> generateSuccTransitionsReflectingSinks(ProductState s) {
        Set<Set<ValuationSet>> product = new HashSet<>();

        product.add(primaryAutomaton.transitions.row(s.getPrimaryState()).keySet());

        for (GOperator slaveFormula : s.getSecondaryMap().keySet()) {
            Automaton<RabinSlave.State> m = secondaryAutomata.get(slaveFormula);
            for (RabinSlave.State fs : m.getStates()) {
                product.add(m.transitions.row(fs).keySet());
            }
        }

        product.removeIf(Set::isEmpty); // removing empty trans due to sinks
        return generatePartitioning(product);
    }

    @Override
    protected @NotNull Product.ProductState generateInitialState() {
        return new ProductState(primaryAutomaton.getInitialState(), relevantSecondarySlaves(primaryAutomaton.getInitialState()),
                k -> secondaryAutomata.get(k).getInitialState());
    }


    private Set<GOperator> relevantSecondarySlaves(@NotNull Master.State primaryState) {
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

        private ProductState(Master.State primaryState, Map<GOperator, RabinSlave.State> secondaryStates) {
            super(primaryState, secondaryStates);
        }

        private ProductState(Master.State primaryState, Collection<GOperator> keys, Function<GOperator, RabinSlave.State> constructor) {
            super(primaryState, keys, constructor);
        }

        @Override
        public @NotNull ValuationSetFactory getFactory() {
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
        protected ProductState constructState(Master.State primaryState, Map<GOperator, RabinSlave.State> secondaryStates) {
            return new ProductState(primaryState, secondaryStates);
        }

        @Override
        protected Map<ValuationSet, Map<GOperator, RabinSlave.State>> secondaryJointMove() {
            Map<GOperator, RabinSlave> secondary = getSecondaryAutomata();
            Map<ValuationSet, Map<GOperator, RabinSlave.State>> current = new HashMap<>();
            current.put(getFactory().createUniverseValuationSet(), Collections.emptyMap());

            Map<GOperator, RabinSlave.State> keys;
            if (!secondaryStates.isEmpty()) {
                keys = new HashMap<>(secondaryStates);
            } else {
                keys = new HashMap<>();
                for (Map.Entry<GOperator, RabinSlave> secAut : secondaryAutomata.entrySet()) {
                    keys.put(secAut.getKey(), secAut.getValue().getInitialState());
                }
            }

            for (Map.Entry<GOperator, RabinSlave.State> entry : keys.entrySet()) {
                GOperator key = entry.getKey();
                RabinSlave.State state = entry.getValue();

                Map<ValuationSet, RabinSlave.State> successors;
                if (!secondary.isEmpty()) {
                    successors = secondary.get(key).getSuccessors(state);
                } else {
                    successors = secondaryAutomata.get(key).getSuccessors(state);
                }
                Map<ValuationSet, Map<GOperator, RabinSlave.State>> next = new HashMap<>();

                for (Map.Entry<ValuationSet, Map<GOperator, RabinSlave.State>> entry1 : current.entrySet()) {
                    for (Map.Entry<ValuationSet, RabinSlave.State> entry2 : successors.entrySet()) {
                        ValuationSet set = entry1.getKey().clone();
                        set.retainAll(entry2.getKey());

                        if (!set.isEmpty()) {
                            Map<GOperator, RabinSlave.State> states = new HashMap<>(entry1.getValue());
                            states.put(key, entry2.getValue());

                            next.put(set, states);
                        }
                    }
                }

                current = next;
            }

            return current;
        }
    }
}
