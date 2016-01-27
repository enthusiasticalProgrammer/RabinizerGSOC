package rabinizer.automata.nxt;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rabinizer.automata.*;
import rabinizer.automata.output.HOAConsumerExtended;
import rabinizer.ltl.*;
import rabinizer.ltl.bdd.BDDEquivalenceClassFactory;
import rabinizer.ltl.bdd.BDDValuationSetFactory;

import java.util.*;

public class DetLimitAutomaton {

    private static final JumpVisitor JUMP_VISITOR = new JumpVisitor();
    private static final SkeletonVisitor SKELETON_VISITOR = new SkeletonVisitor();

    private final Formula initialFormula;
    private final IState<?> initialState;
    private final DetComponent detComponent;
    private final NonDetComponent nonDetComponent;
    private final int acceptanceConditionSize;
    private final EquivalenceClassFactory equivalenceClassFactory;
    private final ValuationSetFactory valuationSetFactory;
    private final boolean skeleton;

    public DetLimitAutomaton(Formula formula) {
        this(formula, EnumSet.allOf(Optimisation.class));
    }

    public DetLimitAutomaton(Formula formula, Collection<Optimisation> optimisations) {
        initialFormula = Simplifier.simplify(formula, Simplifier.Strategy.MODAL_EXT);

        equivalenceClassFactory = new BDDEquivalenceClassFactory(initialFormula.getPropositions());
        valuationSetFactory = new BDDValuationSetFactory(initialFormula.getAtoms());

        nonDetComponent = new NonDetComponent(equivalenceClassFactory, valuationSetFactory, optimisations);
        detComponent = new DetComponent(new DetLimitMaster(equivalenceClassFactory, valuationSetFactory, optimisations, false), valuationSetFactory, optimisations);

        skeleton = optimisations.contains(Optimisation.SKELETON);

        if (skeleton) {
            acceptanceConditionSize = Math.max(initialFormula.accept(SKELETON_VISITOR).stream().mapToInt(Set::size).max().orElse(1), 1);
        } else {
            acceptanceConditionSize = Math.max(initialFormula.gSubformulas().size(), 1);
        }

        EquivalenceClass semiClazz = equivalenceClassFactory.createEquivalenceClass(initialFormula);

        Set<Set<GOperator>> keys = skeleton ? initialFormula.accept(SKELETON_VISITOR) : Sets.powerSet(initialFormula.gSubformulas());

        if (isImpatientState(semiClazz) && keys.size() <= 1) {
            Set<GOperator> key = keys.iterator().next();
            Visitor<Formula> fullVisitor = new GSubstitutionVisitor(g -> BooleanConstant.get(key.contains(g)));
            EquivalenceClass fullClazz = equivalenceClassFactory.createEquivalenceClass(Simplifier.simplify(initialFormula.accept(fullVisitor), Simplifier.Strategy.MODAL_EXT));
            DetComponent.State initialState = detComponent.jump(fullClazz, key);
            detComponent.generate(initialState);
            this.initialState = initialState;
        } else {
            Master.State initialState = nonDetComponent.generateInitialState(semiClazz);
            nonDetComponent.generate(initialState);
            this.initialState = initialState;
        }
    }

    private static boolean isImpatientState(EquivalenceClass clazz) {
        if (clazz.isTrue()) {
            return true;
        }

        Formula representative = clazz.getRepresentative();
        return representative.accept(JUMP_VISITOR);
    }

    private static boolean isPatientState(EquivalenceClass clazz) {
        Formula representative = clazz.getRepresentative();
        return representative.getTopMostPropositions().stream().allMatch(e -> !(e instanceof GOperator));
    }

    public int size() {
        return detComponent.size() + nonDetComponent.size();
    }

    public void toHOA(HOAConsumer c) throws HOAConsumerException {
        HOAConsumerExtended<IState<?>> consumer = new HOAConsumerExtended<>(c, HOAConsumerExtended.AutomatonType.TRANSITION);

        consumer.setHeader(initialFormula, valuationSetFactory.getAlphabet());
        consumer.setGenBuchiAcceptance(acceptanceConditionSize);
        consumer.setInitialState(initialState);

        nonDetComponent.toHOA(consumer);
        detComponent.toHOA(consumer);
        consumer.done();
    }

    private static class JumpVisitor implements Visitor<Boolean> {

        @Override
        public Boolean defaultAction(@NotNull Formula formula) {
            return formula.gSubformulas().isEmpty();
        }

        @Override
        public Boolean visit(@NotNull Conjunction conjunction) {
            return conjunction.children.stream().allMatch(e -> e.accept(this));
        }

        @Override
        public Boolean visit(@NotNull GOperator gOperator) {
            if (gOperator.operand.gSubformulas().isEmpty()) {
                return true;
            }

            return gOperator.operand.accept(this);
        }
    }

    class NonDetComponent extends DetLimitMaster {
        NonDetComponent(EquivalenceClassFactory equivalenceClassFactory, ValuationSetFactory valuationSetFactory, Collection<Optimisation> optimisations) {
            super(equivalenceClassFactory, valuationSetFactory, optimisations, true);
            generate();
        }

        @Override
        public int size() {
            return Math.max(states.size() - 1, 0);
        }

        void toHOA(HOAConsumerExtended<IState<?>> consumer) throws HOAConsumerException {
            for (Master.State state : states) {
                // Skip accepting sink
                if (state.getClazz().isTrue()) {
                    continue;
                }

                // Jump is required.
                final boolean impatient = isImpatientState(state.getClazz());
                // Jump can be delayed.
                final boolean patient = isPatientState(state.getClazz());

                consumer.addState(state);

                for (Set<String> valuation : Sets.powerSet(valuationSetFactory.getAlphabet())) {
                    Master.State successor = getSuccessor(state, valuation);
                    EquivalenceClass successorClazz;

                    if (successor != null) {
                        successorClazz = successor.getClazz();
                    } else if (impatient) {
                        successorClazz = step(state.getClazz(), valuation);
                    } else {
                        continue;
                    }

                    if (successorClazz.isTrue()) {
                        DetComponent.State acceptingSink = detComponent.jump(successorClazz, Collections.emptySet());
                        consumer.addEdge(state, valuation, acceptingSink);
                    } else {
                        // We can stay in the non-deterministic component.
                        if (!impatient) {
                            consumer.addEdge(state, valuation, successor);
                        }

                        // Waiting may be hurtful. Let's add a jump to the deterministic component.
                        if (!patient) {
                            Formula stateFormula = state.getClazz().getRepresentative();
                            Set<Set<GOperator>> keys = skeleton ? stateFormula.accept(SKELETON_VISITOR) : Sets.powerSet(stateFormula.gSubformulas());

                            for (Set<GOperator> key : keys) {
                                DetComponent.State successor2 = detComponent.jump(state.getClazz(), key, valuation);

                                if (successor2 != null) {
                                    consumer.addEdge(state, valuation, successor2);
                                }
                            }
                        }
                    }
                }

                consumer.stateDone();
            }
        }

        @Override
        protected boolean suppressEdge(EquivalenceClass current, Set<String> valuation, EquivalenceClass successor) {
            return successor.isFalse() || isImpatientState(current);
        }
    }

    class DetComponent extends Automaton<DetComponent.State> {

        final DetLimitMaster primaryAutomaton;
        final Map<Set<GOperator>, Map<GOperator, DetLimitSlave>> secondaryAutomata;
        final Collection<Optimisation> optimisations;
        final Table<Set<GOperator>, GOperator, Integer> acceptanceIndexMapping;

        DetComponent(DetLimitMaster primaryAutomaton, ValuationSetFactory valuationSetFactory, Collection<Optimisation> optimisations) {
            super(valuationSetFactory, false);
            this.primaryAutomaton = primaryAutomaton;
            secondaryAutomata = new HashMap<>();
            secondaryAutomata.put(Collections.emptySet(), Collections.emptyMap());
            this.optimisations = optimisations;
            acceptanceIndexMapping = HashBasedTable.create();
        }

        State jump(EquivalenceClass master, Set<GOperator> keys) {
            return jump(master, keys, null);
        }

        @Nullable State jump(@NotNull EquivalenceClass master, @NotNull Set<GOperator> keys, @Nullable Set<String> valuation) {
            Master.State primaryState = getPrimaryState(master, keys, valuation);
            Map<GOperator, DetLimitSlave.State> secondaryStateMap = getSecondaryStateMap(keys, valuation);

            if (primaryState == null || secondaryStateMap == null) {
                return null;
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
                Visitor<Formula> visitor = new GSubstitutionVisitor(g -> BooleanConstant.get(keys.contains(g)));

                int i = 0;

                for (GOperator key : keys) {
                    Formula initialFormula = Simplifier.simplify(key.operand.accept(visitor), Simplifier.Strategy.MODAL_EXT);
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
            Visitor<Formula> visitor = new GSubstitutionVisitor(g -> BooleanConstant.get(keys.contains(g)));

            if (valuation != null) {
                Formula formula = Simplifier.simplify(master.getRepresentative().accept(visitor), Simplifier.Strategy.MODAL_EXT);
                Conjunction facts = new Conjunction(keys.stream().map(key -> Simplifier.simplify(key.operand.accept(visitor), Simplifier.Strategy.MODAL_EXT)));
                Visitor<Formula> evaluateVisitor = new EvaluateVisitor(equivalenceClassFactory, facts);
                formula = Simplifier.simplify(formula.accept(evaluateVisitor), Simplifier.Strategy.MODAL_EXT);

                Master.State preState = primaryAutomaton.generateInitialState(equivalenceClassFactory.createEquivalenceClass(formula));
                return primaryAutomaton.getSuccessor(preState, valuation);
            }

            Formula formula = Simplifier.simplify(master.getRepresentative().accept(visitor), Simplifier.Strategy.MODAL_EXT);
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


