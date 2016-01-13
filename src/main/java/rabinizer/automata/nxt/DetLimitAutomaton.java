package rabinizer.automata.nxt;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import jhoafparser.ast.AtomAcceptance;
import jhoafparser.ast.AtomLabel;
import jhoafparser.ast.BooleanExpression;
import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import rabinizer.automata.*;
import rabinizer.ltl.*;
import rabinizer.ltl.bdd.BDDEquivalenceClassFactory;
import rabinizer.ltl.bdd.BDDValuationSetFactory;

import java.util.*;

public class DetLimitAutomaton {

    protected final Formula initialFormula;
    protected final Set<Master.State> initialStates;

    protected final DetComponent detComponent;
    protected final NonDetComponent nonDetComponent;

    protected final int acceptanceConditionSize;

    protected final EquivalenceClassFactory equivalenceClassFactory;
    protected final ValuationSetFactory valuationSetFactory;

    public DetLimitAutomaton(Formula formula, Collection<Optimisation> optimisations) {
        formula = Simplifier.simplify(formula, Simplifier.Strategy.MODAL_PULLUP_X);

        initialFormula = formula;

        Set<Formula> propositions = new HashSet<>();

        for (Set<GOperator> gSet : Sets.powerSet(formula.gSubformulas())) {
            Visitor<Formula> visitor = new GSubstitutionVisitor(g -> gSet.contains(g) ? null : BooleanConstant.FALSE);
            Visitor<Formula> visitor2 = new GSubstitutionVisitor(g -> gSet.contains(g) ? BooleanConstant.TRUE : BooleanConstant.FALSE);

            collectPropositions(propositions, Simplifier.simplify(formula.accept(visitor)).getPropositions());
            collectPropositions(propositions, Simplifier.simplify(formula.accept(visitor2)).getPropositions());
        }

        equivalenceClassFactory = new BDDEquivalenceClassFactory(propositions);
        valuationSetFactory = new BDDValuationSetFactory(initialFormula.getAtoms());

        nonDetComponent = new NonDetComponent(equivalenceClassFactory, valuationSetFactory, optimisations);
        detComponent = new DetComponent(nonDetComponent, valuationSetFactory, optimisations);

        Set<Formula> initialFormulas;

        if (optimisations.contains(Optimisation.OR_BREAKUP) && formula instanceof Disjunction) {
            Disjunction dis = (Disjunction) formula;
            initialFormulas = dis.getChildren();
        } else {
            initialFormulas = Collections.singleton(formula);
        }

        initialStates = new HashSet<>();

        int accSizeCounter = 1;

        for (Formula initialFormula : initialFormulas) {
            Set<Set<GOperator>> keys = optimisations.contains(Optimisation.SKELETON) ? computeSkeletonKeys(initialFormula) : Sets.powerSet(initialFormula.gSubformulas());

            for (Set<GOperator> key : keys) {
                Visitor<Formula> visitor = new GSubstitutionVisitor(g -> key.contains(g) ? null : BooleanConstant.FALSE);
                EquivalenceClass initial = equivalenceClassFactory.createEquivalenceClass(Simplifier.simplify(initialFormula.accept(visitor)));

                if (!initial.isFalse()) {
                    Master.State initialState = nonDetComponent.generateInitialState(initial);
                    nonDetComponent.generate(initialState);
                    initialStates.add(initialState);
                    accSizeCounter = Math.max(accSizeCounter, key.size() + 1);
                }
            }
        }

        acceptanceConditionSize = accSizeCounter;
    }

    private Set<Set<GOperator>> computeSkeletonKeys(Formula formula) {
        Formula skeleton = formula.accept(new SkeletonVisitor());
        EquivalenceClass skeletonClazz = equivalenceClassFactory.createEquivalenceClass(skeleton);

        Set<Set<GOperator>> keys = new HashSet<>();

        for (Set<GOperator> key : Sets.powerSet(formula.gSubformulas())) {
            EquivalenceClass keyClazz = equivalenceClassFactory.createEquivalenceClass(new Conjunction(key));

            if (keyClazz.implies(skeletonClazz)) {
                keys.add(key);
            }
        }

        return keys;
    }

    private static <T> List<T> toList(Collection<T> collection) {
        return new ArrayList<>(collection);
    }

    private static AtomAcceptance mkInf(int i) {
        return new AtomAcceptance(AtomAcceptance.Type.TEMPORAL_INF, i, false);
    }

    private static BooleanExpression<AtomAcceptance> mkInfAnd(int j) {
        BooleanExpression<AtomAcceptance> conjunction = new BooleanExpression<>(mkInf(0));

        for (int i = 1; i < j; i++) {
            conjunction = conjunction.and(new BooleanExpression<>(mkInf(i)));
        }

        return conjunction;
    }

    public int size() {
        return detComponent.size() + nonDetComponent.size() - 1;
    }

    public void toHOA(final HOAConsumer consumer) throws HOAConsumerException {
        Map<Object, Integer> ids = new HashMap<>();

        consumer.notifyHeaderStart("v1");
        consumer.setTool("Rabinizer", "infty");
        consumer.setName("Automaton for " + initialFormula);

        for (Master.State e : initialStates) {
            consumer.addStartStates(Collections.singletonList(Util.getId(ids, e)));
        }

        consumer.provideAcceptanceName("generalized-Buchi", Collections.singletonList(acceptanceConditionSize));
        consumer.setAcceptanceCondition(acceptanceConditionSize, mkInfAnd(acceptanceConditionSize));

        List<String> alphabetList = toList(valuationSetFactory.getAlphabet());

        consumer.setAPs(alphabetList);
        for (String letter : alphabetList) {
            consumer.addAlias(letter, new BooleanExpression<>(AtomLabel.createAPIndex(alphabetList.indexOf(letter))));
        }

        consumer.notifyBodyStart();

        nonDetComponent.toHOA(consumer, ids);
        detComponent.toHOA(consumer, ids);

        consumer.notifyEnd();
    }

    private static void collectPropositions(Set<Formula> set, Set<Formula> newElements) {
        for (Formula element : newElements) {
            if (!set.contains(element.not())) {
                set.add(element);
            }

            if (element instanceof UOperator) {
                collectPropositions(set, Collections.singleton(new FOperator(((UOperator) element).right)));
            }
        }
    }

    class NonDetComponent extends DetLimitMaster {
        final private boolean delayedJump;

        NonDetComponent(EquivalenceClassFactory equivalenceClassFactory, ValuationSetFactory valuationSetFactory, Collection<Optimisation> optimisations) {
            super(BooleanConstant.TRUE, equivalenceClassFactory, valuationSetFactory, optimisations, true);
            generate();
            delayedJump = optimisations.contains(Optimisation.DELAYED_JUMP);
        }

        protected void toHOA(HOAConsumer consumer, Map<Object, Integer> stateIDs) throws HOAConsumerException {
            Set<Master.State> delayedStates = new HashSet<>();

            if (delayedJump) {
                for (Master.State init : initialStates) {
                    for (Set<Master.State> SCC : SCCAnalyser.SCCs(this, init)) {
                        if (SCC.size() == 1) {
                            Master.State singleState = SCC.iterator().next();
                            if (!isLooping(singleState)) {
                                delayedStates.add(singleState);
                            }
                        }
                    }
                }
            }

            for (Master.State masterState : states) {
                // Skip accepting sink
                if (masterState.getClazz().isTrue()) {
                    continue;
                }

                consumer.addState(Util.getId(stateIDs, masterState), masterState.toString(), null, null);

                for (Map.Entry<ValuationSet, Master.State> entry : transitions.row(masterState).entrySet()) {
                    BooleanExpression<AtomLabel> edgeLabel = Simplifier.simplify(entry.getKey().toFormula()).accept(Util.converter);
                    Master.State successor = entry.getValue();

                    if (successor.getClazz().isTrue()) {
                        DetComponent.State acceptingSink = detComponent.jump(successor, Collections.emptySet());
                        consumer.addEdgeWithLabel(Util.getId(stateIDs, masterState), edgeLabel, Collections.singletonList(Util.getId(stateIDs, acceptingSink)), null);
                    } else  {
                        consumer.addEdgeWithLabel(Util.getId(stateIDs, masterState), edgeLabel, Collections.singletonList(Util.getId(stateIDs, successor)), null);

                        if (!delayedStates.contains(successor) && !successor.getClazz().getRepresentative().gSubformulas().isEmpty()) {
                            DetComponent.State initialState = detComponent.jump(successor, successor.getClazz().getRepresentative().gSubformulas());
                            consumer.addEdgeWithLabel(Util.getId(stateIDs, masterState), edgeLabel, Collections.singletonList(Util.getId(stateIDs, initialState)), null);
                        }
                    }
                }
            }
        }
    }

    class DetComponent extends Automaton<DetComponent.State> {

        protected final DetLimitMaster primaryAutomaton;
        protected final Map<Set<GOperator>, Map<GOperator, DetLimitSlave>> secondaryAutomata;
        protected final Map<Set<GOperator>, EquivalenceClass> GConjunctions;
        protected final Collection<Optimisation> optimisations;
        protected final Table<Set<GOperator>, GOperator, Integer> acceptanceIndexMapping;
        protected final Table<DetLimitSlave.State, ValuationSet, Optional<Integer>> acceptanceCache;

        public DetComponent(DetLimitMaster primaryAutomaton, ValuationSetFactory valuationSetFactory, Collection<Optimisation> optimisations) {
            super(valuationSetFactory, false);

            this.primaryAutomaton = primaryAutomaton;

            GConjunctions = new HashMap<>();
            secondaryAutomata = new HashMap<>();

            GConjunctions.put(Collections.emptySet(), equivalenceClassFactory.getTrue());
            secondaryAutomata.put(Collections.emptySet(), Collections.emptyMap());
            this.optimisations = optimisations;
            acceptanceIndexMapping = HashBasedTable.create();
            acceptanceCache = HashBasedTable.create();
        }

        @Override
        protected State generateInitialState() {
            throw new UnsupportedOperationException();
        }

        protected State jump(Master.State master, Set<GOperator> keys) {
            Map<GOperator, DetLimitSlave> secondaryAutomataMap = secondaryAutomata.get(keys);

            if (secondaryAutomataMap == null) {
                secondaryAutomataMap = new HashMap<>(keys.size());

                Visitor<Formula> slaveVisitor = new GSubstitutionVisitor(g -> keys.contains(g) ? BooleanConstant.TRUE : abort());

                int i = 1;

                for (GOperator key : keys) {
                    DetLimitSlave slave = new DetLimitSlave(Simplifier.simplify(key.getOperand().accept(slaveVisitor)), equivalenceClassFactory, valuationSetFactory, optimisations);
                    secondaryAutomataMap.put(key, slave);
                    acceptanceIndexMapping.put(keys, key, i);
                    i++;
                }

                GConjunctions.put(keys, equivalenceClassFactory.createEquivalenceClass(new Conjunction(keys)));
                secondaryAutomata.put(keys, secondaryAutomataMap);
            }

            State state = new State(primaryAutomaton.createState(master.getClazz()), secondaryAutomataMap, null);
            generate(state);
            return state;
        }

        protected void toHOA(HOAConsumer consumer, Map<Object, Integer> stateIDs) throws HOAConsumerException {
            for (State productState : states) {
                consumer.addState(Util.getId(stateIDs, productState), productState.toString(), null, null);

                Map<ValuationSet, List<Integer>> accSetMap = productState.getAcceptance();

                for (Map.Entry<ValuationSet, State> entry : transitions.row(productState).entrySet()) {
                    State successor = entry.getValue();
                    ValuationSet valuationSet = entry.getKey();
                    BooleanExpression<AtomLabel> edgeLabel = valuationSet.toFormula().accept(Util.converter);

                    List<Integer> accSet = null;

                    for (Map.Entry<ValuationSet, List<Integer>> foo : accSetMap.entrySet()) {
                        if (foo.getKey().containsAll(valuationSet)) {
                            accSet = new ArrayList<>();
                            accSet.addAll(foo.getValue());
                        }
                    }

                    if (accSet == null) {
                        throw new IllegalStateException();
                    }

                    for (int i = productState.getSecondaryMap().size() + 1; i < acceptanceConditionSize; i++) {
                       accSet.add(i);
                    }

                    consumer.addEdgeWithLabel(Util.getId(stateIDs, productState), edgeLabel, Collections.singletonList(Util.getId(stateIDs, successor)), accSet);

                }
            }
        }

        private BooleanConstant abort() {
            throw new IllegalArgumentException();
        }

        public class State extends AbstractProductState<Master.State, GOperator, DetLimitSlave.State, State> implements IState<State> {

            public State(Master.State primaryState, Map<GOperator, DetLimitSlave.State> secondaryStates) {
                super(primaryState, secondaryStates);
            }

            public State(Master.State primaryState, Map<GOperator, DetLimitSlave> secondaryAutomata, Void fix) {
                super(primaryState, secondaryAutomata.keySet(), key -> secondaryAutomata.get(key).getInitialState());
            }

            public Map<ValuationSet, List<Integer>> getAcceptance() {
                EquivalenceClass slaveConjunction = secondaryStates.values().stream().map(s -> s.next.and(s.current)).reduce(equivalenceClassFactory.getTrue(), EquivalenceClass::and);
                EquivalenceClass antecedent = GConjunctions.get(secondaryStates.keySet()).and(slaveConjunction);
                boolean primaryAccepts = antecedent.implies(primaryState.getClazz());

                Map<ValuationSet, List<Integer>> current = new HashMap<>();
                current.put(createUniverseValuationSet(), primaryAccepts ? Collections.singletonList(0) : Collections.emptyList());

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

                return current;
            }

            @Override
            public State getSuccessor(Set<String> valuation) {
                State successor = super.getSuccessor(valuation);

                if (successor != null && successor.primaryState.getClazz().isTrue()) {
                    return new State(successor.primaryState, Collections.emptyMap());
                }

                return successor;
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

            @Override
            protected ValuationSet createUniverseValuationSet() {
                return valuationSetFactory.createUniverseValuationSet();
            }

            @Override
            public ValuationSetFactory getFactory() {
                return valuationSetFactory;
            }
        }
    }
}


