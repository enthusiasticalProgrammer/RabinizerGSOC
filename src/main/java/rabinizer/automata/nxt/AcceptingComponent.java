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

package rabinizer.automata.nxt;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Table;
import jhoafparser.consumer.HOAConsumerException;

import rabinizer.automata.*;
import rabinizer.automata.output.HOAConsumerExtended;
import rabinizer.collections.valuationset.ValuationSet;
import rabinizer.collections.valuationset.ValuationSetFactory;
import rabinizer.ltl.Conjunction;
import rabinizer.ltl.Formula;
import rabinizer.ltl.GOperator;
import rabinizer.ltl.Visitor;
import rabinizer.ltl.equivalence.EquivalenceClass;
import rabinizer.ltl.equivalence.EquivalenceClassFactory;
import rabinizer.ltl.equivalence.EvaluateVisitor;
import rabinizer.ltl.simplifier.Simplifier;

import javax.annotation.Nonnegative;
import javax.annotation.Nullable;
import java.util.*;

public class AcceptingComponent extends Automaton<AcceptingComponent.State> {

    private final EquivalenceClassFactory equivalenceClassFactory;
    private final Master primaryAutomaton;
    private final Map<Set<GOperator>, Map<GOperator, DetLimitSlave>> secondaryAutomata;
    private final Collection<Optimisation> optimisations;
    private final Table<Set<GOperator>, GOperator, Integer> acceptanceIndexMapping;
    private final LoadingCache<AcceptingComponent.State, Map<ValuationSet, BitSet>> acceptanceCache;

    @Nonnegative
    int acceptanceConditionSize;

    AcceptingComponent(Master primaryAutomaton, EquivalenceClassFactory factory, ValuationSetFactory valuationSetFactory, Collection<Optimisation> optimisations) {
        super(valuationSetFactory);
        this.primaryAutomaton = primaryAutomaton;
        secondaryAutomata = new HashMap<>();
        secondaryAutomata.put(Collections.emptySet(), Collections.emptyMap());
        this.optimisations = optimisations;
        acceptanceIndexMapping = HashBasedTable.create();
        equivalenceClassFactory = factory;
        acceptanceConditionSize = 1;
        acceptanceCache = CacheBuilder.newBuilder().build(new AcceptanceCacheLoader());
    }

    public int getAcceptanceSize() {
        return acceptanceConditionSize;
    }

    void jumpInitial(EquivalenceClass master, Set<GOperator> keys) {
        initialState = jump(master, keys);

        if (initialState == null) {
            initialState = new State(primaryAutomaton.generateInitialState(equivalenceClassFactory.getFalse()), ImmutableMap.of());
        }
    }

    @Nullable State jump(EquivalenceClass master, Set<GOperator> keys) {
        Master.State primaryState = getPrimaryState(master, keys);
        ImmutableMap<GOperator, DetLimitSlave.State> secondaryStateMap = getSecondaryStateMap(keys);

        if (primaryState == null || secondaryStateMap == null) {
            return null;
        }

        // Increase the number of Buchi acceptance conditions.
        if (keys.size() > acceptanceConditionSize) {
            acceptanceConditionSize = keys.size();
        }

        State state = new State(primaryState, secondaryStateMap);
        generate(state);
        return state;
    }

    void toHOA(HOAConsumerExtended<IState<?>> consumer) throws HOAConsumerException {
        for (State productState : getStates()) {
            consumer.addState(productState);

            Map<ValuationSet, BitSet> accSetMap = getAcceptance(productState);

            transitions.get(productState).forEach((successor, valuationSet) -> {
                for (Map.Entry<ValuationSet, BitSet> acceptance : accSetMap.entrySet()) {
                    ValuationSet label = acceptance.getKey().intersect(valuationSet);

                    if (!label.isEmpty()) {
                        try {
                            consumer.addEdge(productState, label.toFormula(), successor, acceptance.getValue());
                        } catch (HOAConsumerException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

            consumer.stateDone();
        }
    }

    private @Nullable Map<GOperator, DetLimitSlave> getSecondaryAutomatonMap(Set<GOperator> keys) {
        Map<GOperator, DetLimitSlave> secondaryAutomatonMap = secondaryAutomata.get(keys);

        if (secondaryAutomatonMap == null) {
            secondaryAutomatonMap = new HashMap<>(keys.size());
            int i = 0;

            for (GOperator key : keys) {
                Formula initialFormula = Simplifier.simplify(key.operand.evaluate(keys, Formula.EvaluationStrategy.LTL), Simplifier.Strategy.MODAL);
                EquivalenceClass initialClazz = equivalenceClassFactory.createEquivalenceClass(initialFormula);

                if (initialClazz.isFalse()) {
                    return null;
                }

                DetLimitSlave slave = new DetLimitSlave(initialClazz, equivalenceClassFactory, valuationSetFactory, optimisations);
                secondaryAutomatonMap.put(key, slave);
                acceptanceIndexMapping.put(keys, key, i);
                i++;
            }

            secondaryAutomata.put(keys, secondaryAutomatonMap);
        }

        return secondaryAutomatonMap;
    }

    private @Nullable ImmutableMap<GOperator, DetLimitSlave.State> getSecondaryStateMap(Set<GOperator> keys) {
        Map<GOperator, DetLimitSlave> secondaryAutomatonMap = getSecondaryAutomatonMap(keys);

        if (secondaryAutomatonMap == null) {
            return null;
        }

        ImmutableMap.Builder<GOperator, DetLimitSlave.State> secondaryStateMap = ImmutableMap.builder();
        secondaryAutomatonMap.forEach((key, slave) -> secondaryStateMap.put(key, slave.getInitialState()));
        return secondaryStateMap.build();
    }

    private @Nullable Master.State getPrimaryState(EquivalenceClass master, Set<GOperator> keys) {
        // TODO: remove simple stuff
        Formula formula = Simplifier.simplify(master.getRepresentative().evaluate(keys, Formula.EvaluationStrategy.LTL), Simplifier.Strategy.MODAL);
        Conjunction facts = new Conjunction(keys.stream().map(key -> Simplifier.simplify(key.operand.evaluate(keys, Formula.EvaluationStrategy.LTL), Simplifier.Strategy.MODAL)));
        Visitor<Formula> evaluateVisitor = new EvaluateVisitor(equivalenceClassFactory, facts);
        formula = Simplifier.simplify(formula.accept(evaluateVisitor), Simplifier.Strategy.MODAL);

        EquivalenceClass preClazz = equivalenceClassFactory.createEquivalenceClass(formula);

        if (preClazz.isFalse()) {
            return null;
        }

        return primaryAutomaton.generateInitialState(preClazz);
    }

    public Map<ValuationSet, BitSet> getAcceptance(AcceptingComponent.State state) {
        return acceptanceCache.getUnchecked(state);
    }

    public class State extends AbstractProductState<Master.State, GOperator, DetLimitSlave.State, State> implements IState<State> {

        public State(Master.State primaryState, ImmutableMap<GOperator, DetLimitSlave.State> secondaryStates) {
            super(primaryState, secondaryStates);
        }

        Map<ValuationSet, BitSet> getAcceptance() {
            ValuationSet universe = valuationSetFactory.createUniverseValuationSet();

            // Don't generate acceptance condition, if we didn't reached true.
            if (!primaryState.getClazz().isTrue()) {
                return Collections.singletonMap(universe, new BitSet(acceptanceConditionSize));
            }

            Map<ValuationSet, BitSet> current = new HashMap<>();
            current.put(universe, new BitSet(acceptanceConditionSize));

            Map<GOperator, DetLimitSlave> secondaryAuto = secondaryAutomata.get(secondaryStates.keySet());
            Map<GOperator, Integer> accMap = acceptanceIndexMapping.row(secondaryStates.keySet());

            for (Map.Entry<GOperator, DetLimitSlave.State> entry : secondaryStates.entrySet()) {
                GOperator key = entry.getKey();
                DetLimitSlave.State state = entry.getValue();

                ValuationSet acceptance = secondaryAuto.get(key).getAcceptance(state);

                Map<ValuationSet, BitSet> next = new HashMap<>();

                for (Map.Entry<ValuationSet, BitSet> entry1 : current.entrySet()) {
                    ValuationSet AandB = entry1.getKey().clone();
                    ValuationSet AandNotB = entry1.getKey().clone();
                    AandB.retainAll(acceptance);
                    AandNotB.removeAll(acceptance);

                    if (!AandB.isEmpty()) {
                        BitSet accList = (BitSet) entry1.getValue().clone();
                        accList.set(accMap.get(key));
                        next.put(AandB, accList);
                    }

                    if (!AandNotB.isEmpty()) {
                        next.put(AandNotB, entry1.getValue());
                    }
                }

                current = next;
            }

            final int size = secondaryStates.size();
            current.forEach((k, v) -> v.set(size, acceptanceConditionSize));
            return current;
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
        protected Map<GOperator, DetLimitSlave> getSecondaryAutomata() {
            return secondaryAutomata.get(secondaryStates.keySet());
        }

        @Override
        protected State constructState(Master.State primaryState, ImmutableMap<GOperator, DetLimitSlave.State> secondaryStates) {
            return new State(primaryState, secondaryStates);
        }
    }

    private static class AcceptanceCacheLoader extends CacheLoader<State, Map<ValuationSet, BitSet>> {
        @Override
        public Map<ValuationSet, BitSet> load(State key) throws Exception {
            return key.getAcceptance();
        }
    }
}
