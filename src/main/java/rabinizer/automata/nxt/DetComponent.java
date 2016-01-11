package rabinizer.automata.nxt;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import jhoafparser.ast.AtomLabel;
import jhoafparser.ast.BooleanExpression;
import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import rabinizer.automata.*;
import rabinizer.ltl.*;

import java.util.*;

public class DetComponent extends Automaton<DetComponent.State> {

    protected final DetLimitMaster primaryAutomaton;
    protected final Map<Set<GOperator>, Map<GOperator, DetLimitSlave>> secondaryAutomata;
    protected final Map<Set<GOperator>, EquivalenceClass> GConjunctions;
    protected final Collection<Optimisation> optimisations;
    protected final EquivalenceClassFactory equivalenceClassFactory;
    protected final Table<Set<GOperator>, GOperator, Integer> acceptanceIndexMapping;

    protected final Table<DetLimitSlave.State, ValuationSet, Optional<Integer>> acceptanceCache;

    public DetComponent(DetLimitMaster primaryAutomaton, EquivalenceClassFactory factory, ValuationSetFactory valuationSetFactory, Collection<Optimisation> optimisations) {
        super(valuationSetFactory, false);

        equivalenceClassFactory = factory;

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
    protected DetComponent.State generateInitialState() {
        throw new UnsupportedOperationException();
    }

    protected DetComponent.State jump(Master.State master, Set<GOperator> keys) {
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

    protected void toHOA(HOAConsumer consumer, Map<Object, Integer> stateIDs, int maxAcc) throws HOAConsumerException {
        for (DetComponent.State productState : states) {
            consumer.addState(Util.getId(stateIDs, productState), productState.toString(), null, null);

            Map<ValuationSet, List<Integer>> accSetMap = productState.getAcceptance();

            for (Map.Entry<ValuationSet, DetComponent.State> entry : transitions.row(productState).entrySet()) {
                DetComponent.State successor = entry.getValue();
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

                for (int i = productState.getSecondaryMap().size() + 1; i < maxAcc; i++) {
                   accSet.add(i);
                }

                consumer.addEdgeWithLabel(Util.getId(stateIDs, productState), edgeLabel, Collections.singletonList(Util.getId(stateIDs, successor)), accSet);

            }
        }
    }

    private BooleanConstant abort() {
        throw new IllegalArgumentException();
    }

    public Map<ValuationSet, List<Integer>> getAcceptance() {
        return null;
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