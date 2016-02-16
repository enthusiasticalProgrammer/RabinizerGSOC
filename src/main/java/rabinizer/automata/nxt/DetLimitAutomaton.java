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

import com.google.common.collect.*;
import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rabinizer.automata.*;
import rabinizer.automata.output.HOAConsumerExtended;
import rabinizer.ltl.equivalence.EquivalenceClass;
import rabinizer.ltl.equivalence.EquivalenceClassFactory;
import rabinizer.ltl.*;
import rabinizer.ltl.equivalence.BDDEquivalenceClassFactory;
import rabinizer.ltl.equivalence.EvaluateVisitor;
import rabinizer.ltl.simplifier.Simplifier;
import rabinizer.collections.valuationset.BDDValuationSetFactory;
import rabinizer.collections.valuationset.ValuationSet;
import rabinizer.collections.valuationset.ValuationSetFactory;

import java.util.*;

public class DetLimitAutomaton {

    private static final JumpVisitor JUMP_VISITOR = new JumpVisitor();
    private static final SkeletonVisitor SKELETON_VISITOR = new SkeletonVisitor();

    /* TODO: Do not hardcode BDD! */
    private final EquivalenceClassFactory equivalenceClassFactory;
    private final ValuationSetFactory valuationSetFactory;

    private final InitComponent initComponent;
    private final AccComponent accComponent;
    private final Table<Master.State, ValuationSet, Set<?>> jumps;

    private int acceptanceConditionSize;

    private final boolean skeleton;
    private final boolean scc;
    private final boolean impatient;

    public DetLimitAutomaton(Formula formula) {
        this(formula, EnumSet.allOf(Optimisation.class));
    }

    public DetLimitAutomaton(Formula formula, Collection<Optimisation> optimisations) {
        formula = Simplifier.simplify(formula, Simplifier.Strategy.MODAL_EXT);
        equivalenceClassFactory = new BDDEquivalenceClassFactory(formula.getPropositions());
        valuationSetFactory = new BDDValuationSetFactory(formula.getAtoms());

        skeleton = optimisations.contains(Optimisation.SKELETON);
        scc = optimisations.contains(Optimisation.SCC);
        impatient = optimisations.contains(Optimisation.IMPATIENT);

        acceptanceConditionSize = 1;
        EquivalenceClass initialClazz = equivalenceClassFactory.createEquivalenceClass(formula);
        Set<Set<GOperator>> keys = skeleton ? formula.accept(SKELETON_VISITOR) : Sets.powerSet(formula.gSubformulas());
        accComponent = new AccComponent(new Master(valuationSetFactory, optimisations), valuationSetFactory, optimisations);

        if (isImpatientState(initialClazz) && keys.size() <= 1) {
            jumps = null;
            initComponent = null;

            Set<GOperator> key = keys.iterator().next();
            initialClazz = equivalenceClassFactory.createEquivalenceClass(Simplifier.simplify(formula.evaluate(key, Formula.EvaluationStrategy.LTL), Simplifier.Strategy.MODAL_EXT));
            accComponent.jumpInitial(initialClazz, key);
            accComponent.generate();
        } else {
            jumps = HashBasedTable.create();
            initComponent = new InitComponent(initialClazz, valuationSetFactory, optimisations);
            initComponent.generate();
        }
    }

    private @NotNull IState<?> getInitialState() {
        if (initComponent != null) {
            return initComponent.getInitialState();
        }

        return accComponent.getInitialState();
    }

    private boolean isImpatientState(EquivalenceClass clazz) {
        if (!impatient) {
            return false;
        }

        if (clazz.isTrue() || clazz.isFalse()) {
            return true;
        }

        Formula representative = clazz.getRepresentative();
        return representative.accept(JUMP_VISITOR);
    }

    public int size() {
        if (initComponent == null) {
            return accComponent.size();
        }

        return accComponent.size() + initComponent.size();
    }

    public void toHOA(HOAConsumer c) throws HOAConsumerException {
        HOAConsumerExtended<IState<?>> consumer = new HOAConsumerExtended<>(c, HOAConsumerExtended.AutomatonType.TRANSITION);
        IState<?> initialState = getInitialState();

        consumer.setHeader(initialState.toString(), valuationSetFactory.getAlphabet());
        consumer.setGenBuchiAcceptance(acceptanceConditionSize);
        consumer.setInitialState(initialState);

        if (initComponent != null) {
            initComponent.toHOA(consumer);
        }

        accComponent.toHOA(consumer);
        consumer.done();
    }

    private static class JumpVisitor implements Visitor<Boolean> {

        @Override
        public Boolean defaultAction(@NotNull Formula formula) {
            return formula.gSubformulas().isEmpty();
        }

        @Override
        public Boolean visit(@NotNull Conjunction conjunction) {
            return conjunction.allMatch(e -> e.accept(this));
        }

        @Override
        public Boolean visit(@NotNull GOperator gOperator) {
            if (gOperator.operand.gSubformulas().isEmpty()) {
                return true;
            }

            return gOperator.operand.accept(this);
        }
    }

    class InitComponent extends Master {
        InitComponent(EquivalenceClass initialClazz, ValuationSetFactory valuationSetFactory, Collection<Optimisation> optimisations) {
            super(initialClazz, valuationSetFactory, optimisations, true);
        }

        @Override
        public void generate() {
            super.generate();

            // Generate Jump Table
            List<Set<Master.State>> sccs = scc ? SCCs() : Collections.singletonList(states);

            for (Set<Master.State> scc : sccs) {
                // Skip non-looping states of with no impatient successors of a singleton SCC.
                if (scc.size() == 1) {
                    Master.State state = scc.iterator().next();

                    if (!isLooping(state) && isComplete(state)) {
                        continue;
                    }
                }

                for (Master.State state : scc) {
                    Formula stateFormula = state.getClazz().getRepresentative();
                    Set<Set<GOperator>> keys = skeleton ? stateFormula.accept(SKELETON_VISITOR) : Sets.powerSet(stateFormula.gSubformulas());
                    Map<AccComponent.State, ValuationSet> revMap = new HashMap<>();

                    for (Set<String> valuation : Sets.powerSet(valuationSetFactory.getAlphabet())) {
                        for (Set<GOperator> key : keys) {
                            AccComponent.State successor = accComponent.jump(state.getClazz(), key, valuation);

                            if (successor == null) {
                                continue;
                            }

                            ValuationSet valuationSet = revMap.remove(successor);

                            if (valuationSet == null) {
                                valuationSet = valuationSetFactory.createEmptyValuationSet();
                            }

                            valuationSet.add(valuation);
                            revMap.put(successor, valuationSet);
                        }
                    }

                    for (Map.Entry<AccComponent.State, ValuationSet> entries : revMap.entrySet()) {
                        Set<AccComponent.State> set = (Set<AccComponent.State>) jumps.get(state, entries.getValue());

                        if (set == null) {
                            set = new HashSet<>();
                        }

                        set.add(entries.getKey());
                        jumps.put(state, entries.getValue(), set);
                    }
                }
            }
        }

        void toHOA(HOAConsumerExtended<IState<?>> consumer) throws HOAConsumerException {
            for (Master.State state : states) {
                consumer.addState(state);
                consumer.addEdges(state, getSuccessors(state));
                consumer.addEdges2(state, jumps.row(state));
                consumer.stateDone();
            }
        }

        private boolean isComplete(Master.State state) {
            ValuationSet valuationSet = valuationSetFactory.createEmptyValuationSet();

            for (ValuationSet vs : transitions.row(state).keySet()) {
                valuationSet.addAll(vs);
            }

            return valuationSet.complement().stream().allMatch(vs -> step(state.getClazz(), vs).isFalse());
        }

        @Override
        protected boolean suppressEdge(EquivalenceClass current, Set<String> valuation, EquivalenceClass successor) {
            return successor.isFalse() || isImpatientState(current) || isImpatientState(successor);
        }
    }

    class AccComponent extends Automaton<AccComponent.State> {

        final Master primaryAutomaton;
        final Map<Set<GOperator>, Map<GOperator, DetLimitSlave>> secondaryAutomata;
        final Collection<Optimisation> optimisations;
        final Table<Set<GOperator>, GOperator, Integer> acceptanceIndexMapping;

        AccComponent(Master primaryAutomaton, ValuationSetFactory valuationSetFactory, Collection<Optimisation> optimisations) {
            super(valuationSetFactory, false);
            this.primaryAutomaton = primaryAutomaton;
            secondaryAutomata = new HashMap<>();
            secondaryAutomata.put(Collections.emptySet(), Collections.emptyMap());
            this.optimisations = optimisations;
            acceptanceIndexMapping = HashBasedTable.create();
        }

        void jumpInitial(EquivalenceClass master, Set<GOperator> keys) {
            initialState = jump(master, keys, null);
        }

        @Nullable State jump(@NotNull EquivalenceClass master, @NotNull Set<GOperator> keys, @Nullable Set<String> valuation) {
            Master.State primaryState = getPrimaryState(master, keys, valuation);
            Map<GOperator, DetLimitSlave.State> secondaryStateMap = getSecondaryStateMap(keys, valuation);

            if (primaryState == null || secondaryStateMap == null) {
                return null;
            }

            if (keys.size() > acceptanceConditionSize) {
                acceptanceConditionSize = keys.size();
            }

            State state = new State(primaryState, secondaryStateMap);
            generate(state);
            return state;
        }

        void toHOA(HOAConsumerExtended<IState<?>> consumer) throws HOAConsumerException {
            for (State productState : states) {
                consumer.addState(productState);

                Map<ValuationSet, List<Integer>> accSetMap = productState.getAcceptance();

                for (Map.Entry<ValuationSet, State> entry : transitions.row(productState).entrySet()) {
                    State successor = entry.getValue();
                    ValuationSet valuationSet = entry.getKey();

                    List<Integer> accSet = null;

                    for (Map.Entry<ValuationSet, List<Integer>> foo : accSetMap.entrySet()) {
                        if (foo.getKey().containsAll(valuationSet)) {
                            accSet = new ArrayList<>(foo.getValue());
                            break;
                        }
                    }

                    if (accSet == null) {
                        throw new IllegalStateException();
                    }

                    consumer.addEdge(productState, valuationSet.toFormula(), successor, accSet);
                }

                consumer.stateDone();
            }
        }

        @Override
        protected @NotNull State generateInitialState() {
            throw new UnsupportedOperationException();
        }

        private @NotNull Map<GOperator, DetLimitSlave> getSecondaryAutomatonMap(Set<GOperator> keys) {
            Map<GOperator, DetLimitSlave> secondaryAutomatonMap = secondaryAutomata.get(keys);

            if (secondaryAutomatonMap == null) {
                secondaryAutomatonMap = new HashMap<>(keys.size());
                int i = 0;

                for (GOperator key : keys) {
                    Formula initialFormula = Simplifier.simplify(key.operand.evaluate(keys, Formula.EvaluationStrategy.LTL), Simplifier.Strategy.MODAL_EXT);
                    DetLimitSlave slave = new DetLimitSlave(initialFormula, equivalenceClassFactory, valuationSetFactory, optimisations);
                    secondaryAutomatonMap.put(key, slave);
                    acceptanceIndexMapping.put(keys, key, i);
                    i++;
                }

                secondaryAutomata.put(keys, secondaryAutomatonMap);
            }

            return secondaryAutomatonMap;
        }

        private @Nullable Map<GOperator, DetLimitSlave.State> getSecondaryStateMap(Set<GOperator> keys, Set<String> valuation) {
            Map<GOperator, DetLimitSlave> secondaryAutomatonMap = getSecondaryAutomatonMap(keys);
            Map<GOperator, DetLimitSlave.State> secondaryStateMap = new HashMap<>(keys.size());

            for (Map.Entry<GOperator, DetLimitSlave> entry : secondaryAutomatonMap.entrySet()) {
                DetLimitSlave slave = entry.getValue();
                DetLimitSlave.State state = slave.getInitialState();

                if (valuation != null) {
                    state = slave.getSuccessor(state, valuation);
                }

                if (state == null) {
                    return null;
                }

                secondaryStateMap.put(entry.getKey(), state);
            }

            return secondaryStateMap;
        }

        private @Nullable Master.State getPrimaryState(@NotNull EquivalenceClass master, @NotNull Set<GOperator> keys, @Nullable Set<String> valuation) {
            if (valuation != null) {
                Formula formula = Simplifier.simplify(master.getRepresentative().evaluate(keys, Formula.EvaluationStrategy.LTL), Simplifier.Strategy.MODAL_EXT);
                Conjunction facts = new Conjunction(keys.stream().map(key -> Simplifier.simplify(key.operand.evaluate(keys, Formula.EvaluationStrategy.LTL), Simplifier.Strategy.MODAL_EXT)));
                Visitor<Formula> evaluateVisitor = new EvaluateVisitor(equivalenceClassFactory, facts);
                formula = Simplifier.simplify(formula.accept(evaluateVisitor), Simplifier.Strategy.MODAL_EXT);

                Master.State preState = primaryAutomaton.generateInitialState(equivalenceClassFactory.createEquivalenceClass(formula));
                return primaryAutomaton.getSuccessor(preState, valuation);
            }

            Formula formula = Simplifier.simplify(master.getRepresentative().evaluate(keys, Formula.EvaluationStrategy.LTL), Simplifier.Strategy.MODAL_EXT);
            EquivalenceClass clazz = equivalenceClassFactory.createEquivalenceClass(formula);
            return primaryAutomaton.generateInitialState(clazz);
        }

        public class State extends AbstractProductState<Master.State, GOperator, DetLimitSlave.State, State> implements IState<State> {

            public State(@NotNull Master.State primaryState, @NotNull Map<GOperator, DetLimitSlave.State> secondaryStates) {
                super(primaryState, secondaryStates);
            }

            public Map<ValuationSet, List<Integer>> getAcceptance() {
                ValuationSet universe = valuationSetFactory.createUniverseValuationSet();

                // Don't generate acceptance condition, if we didn't reached true.
                if (!primaryState.getClazz().isTrue()) {
                    return Collections.singletonMap(universe, Collections.emptyList());
                }

                Map<ValuationSet, List<Integer>> current = new HashMap<>();
                current.put(universe, Lists.newArrayList());

                Map<GOperator, DetLimitSlave> secondaryAuto = secondaryAutomata.get(secondaryStates.keySet());
                Map<GOperator, Integer> accMap = acceptanceIndexMapping.row(secondaryStates.keySet());

                for (Map.Entry<GOperator, DetLimitSlave.State> entry : secondaryStates.entrySet()) {
                    GOperator key = entry.getKey();
                    DetLimitSlave.State state = entry.getValue();

                    ValuationSet acceptance = secondaryAuto.get(key).getAcceptance(state);

                    Map<ValuationSet, List<Integer>> next = new HashMap<>();

                    for (Map.Entry<ValuationSet, List<Integer>> entry1 : current.entrySet()) {
                        ValuationSet AandB = entry1.getKey().clone();
                        ValuationSet AandNotB = entry1.getKey().clone();
                        AandB.retainAll(acceptance);
                        AandNotB.removeAll(acceptance);

                        if (!AandB.isEmpty()) {
                            List<Integer> accList = new ArrayList<>(entry1.getValue());
                            accList.add(accMap.get(key));
                            next.put(AandB, accList);
                        }

                        if (!AandNotB.isEmpty()) {
                            next.put(AandNotB, entry1.getValue());
                        }
                    }

                    current = next;
                }

                for (List<Integer> value : current.values()) {
                    for (int i = getSecondaryMap().size(); i < acceptanceConditionSize; i++) {
                        value.add(i);
                    }
                }

                return current;
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
            protected Map<GOperator, DetLimitSlave> getSecondaryAutomata() {
                return secondaryAutomata.get(secondaryStates.keySet());
            }

            @Override
            protected State constructState(Master.State primaryState, Map<GOperator, DetLimitSlave.State> secondaryStates) {
                return new State(primaryState, secondaryStates);
            }
        }
    }
}


