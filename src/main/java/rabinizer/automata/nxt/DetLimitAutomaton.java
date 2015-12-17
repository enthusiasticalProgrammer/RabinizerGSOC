package rabinizer.automata.nxt;

import com.google.common.collect.Sets;
import jhoafparser.ast.AtomAcceptance;
import jhoafparser.ast.AtomLabel;
import jhoafparser.ast.BooleanExpression;
import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import rabinizer.automata.IState;
import rabinizer.automata.Master;
import rabinizer.automata.Optimisation;
import rabinizer.ltl.*;
import rabinizer.ltl.bdd.BDDEquivalenceClassFactory;
import rabinizer.ltl.bdd.BDDValuationSetFactory;

import java.util.*;
import java.util.function.Function;

public class DetLimitAutomaton {

    private final Formula initialFormula;
    private final Map<Set<GOperator>, DetLimitAutomatonComponent> components;
    private final EquivalenceClassFactory equivalenceClassFactory;
    private final ValuationSetFactory<String> valuationSetFactory;

    public DetLimitAutomaton(Formula formula, Collection<Optimisation> optimisations) {
        initialFormula = formula;

        Set<Set<GOperator>> keys = Sets.powerSet(formula.gSubformulas());
        Set<Formula> props = new HashSet<>();

        for (Set<GOperator> gSet : keys) {
            Visitor<Formula> masterVisitor = new GSubstitutionVisitor(g -> gSet.contains(g) ? null : BooleanConstant.FALSE);
            props.addAll(Simplifier.simplify(initialFormula.accept(masterVisitor)).getPropositions());
        }

        equivalenceClassFactory = new BDDEquivalenceClassFactory(props);
        valuationSetFactory = new BDDValuationSetFactory(initialFormula.getAtoms());
        components = new HashMap<>();

        keys.forEach(k -> components.put(k, new DetLimitAutomatonComponent(initialFormula, k, equivalenceClassFactory, valuationSetFactory, optimisations)));
    }

    static private <T> List<T> toList(Collection<T> collection) {
        return new ArrayList<>(collection);
    }

    static private AtomAcceptance mkInf(int i) {
        return new AtomAcceptance(AtomAcceptance.Type.TEMPORAL_INF, i, false);
    }

    static private BooleanExpression<AtomAcceptance> mkInfAnd(int j) {
        BooleanExpression<AtomAcceptance> conjunction = new BooleanExpression<>(mkInf(0));

        for (int i = 1; i < j; i++) {
            conjunction = conjunction.and(new BooleanExpression<>(mkInf(i)));
        }

        return conjunction;
    }

    public void toHOA(final HOAConsumer consumer) throws HOAConsumerException {
        Map<Object, Integer> ids = new HashMap<>();

        consumer.notifyHeaderStart("1");
        consumer.setTool("Rabinizer", "infty");
        consumer.setName("Automaton for " + initialFormula);

        for (DetLimitAutomatonComponent c : components.values()) {
            IState e = c.nondetComponent.getInitialState();
            consumer.addStartStates(Collections.singletonList(Util.getId(ids, e)));
        }

        consumer.provideAcceptanceName("generalized-Buchi", Collections.singletonList(getAcceptingSetSize()));
        consumer.setAcceptanceCondition(getAcceptingSetSize(), mkInfAnd(getAcceptingSetSize()));

        List<String> alphabetList = toList(valuationSetFactory.getAlphabet());

        consumer.setAPs(alphabetList);
        for (String letter : alphabetList) {
            consumer.addAlias(letter, new BooleanExpression<>(AtomLabel.createAPIndex(alphabetList.indexOf(letter))));
        }

        consumer.notifyBodyStart();

        for (DetLimitAutomatonComponent component : components.values()) {
            component.toHOA(consumer, ids, getAcceptingSetSize());
        }

        consumer.notifyEnd();
    }

    private int getAcceptingSetSize() {
        return initialFormula.gSubformulas().size() + 1;
    }
}

class DetLimitAutomatonComponent {

    final protected DetLimitMaster nondetComponent;
    final protected DetLimitProduct detComponent;
    final private Set<GOperator> gset;
    final private ValuationSetFactory<String> factory;

    protected DetLimitAutomatonComponent(Formula initialFormula, Set<GOperator> Gset, EquivalenceClassFactory equivalenceClassFactory, ValuationSetFactory<String> valuationSetFactory, Collection<Optimisation> optimisations) {
        Visitor<Formula> masterVisitor = new GSubstitutionVisitor(g -> Gset.contains(g) ? null : BooleanConstant.FALSE);
        Visitor<Formula> slaveVisitor = new GSubstitutionVisitor(g -> BooleanConstant.get(Gset.contains(g)));

        factory = valuationSetFactory;
        gset = Gset;
        Formula substInit = Simplifier.simplify(initialFormula.accept(masterVisitor));

        nondetComponent = new DetLimitMaster(substInit, equivalenceClassFactory, valuationSetFactory, optimisations);
        Function<GOperator, DetLimitSlave> constructor = g -> new DetLimitSlave(Simplifier.simplify(g.getOperand().accept(slaveVisitor)), equivalenceClassFactory, valuationSetFactory, optimisations);
        detComponent = new DetLimitProduct(nondetComponent, Gset, constructor, equivalenceClassFactory, valuationSetFactory);
    }

    protected void toHOA(HOAConsumer consumer, Map<Object, Integer> stateIDs, int maxAcc) throws HOAConsumerException {
        nondetComponent.generate();
        Converter converter = new Converter();
        final int sizeSec = detComponent.numberOfSecondary();
        Map<GOperator, Integer> infSetMapping = new HashMap<>();

        for (Master.State masterState : nondetComponent.getStates()) {
            consumer.addState(Util.getId(stateIDs, masterState), masterState.toString(), null, null);

            for (Map.Entry<ValuationSet, Master.State> entry : nondetComponent.getTransitions().row(masterState).entrySet()) {
                Master.State succ = entry.getValue();
                DetLimitProduct.State init = detComponent.generateInitialState(succ);
                detComponent.generate(init);

                BooleanExpression<AtomLabel> edgeLabel = Simplifier.simplify(entry.getKey().toFormula()).accept(converter);

                consumer.addEdgeWithLabel(Util.getId(stateIDs, masterState), edgeLabel, Collections.singletonList(Util.getId(stateIDs, succ)), null);
                consumer.addEdgeWithLabel(Util.getId(stateIDs, masterState), edgeLabel, Collections.singletonList(Util.getId(stateIDs, init)), null);
            }
        }

        for (DetLimitProduct.State productState : detComponent.getStates()) {
            consumer.addState(Util.getId(stateIDs, productState), productState.toString(), null, null);

            for (Map.Entry<ValuationSet, DetLimitProduct.State> entry : detComponent.getTransitions().row(productState).entrySet()) {
                DetLimitProduct.State succ = entry.getValue();

                for (Set<String> valuation : entry.getKey()) {
                    BooleanExpression<AtomLabel> edgeLabel = Simplifier.simplify(factory.createValuationSet(valuation).toFormula()).accept(converter);

                    List<Integer> accSet = new ArrayList<>();

                    if (productState.isAccepting(valuation)) {
                        accSet.add(0);
                    }

                    for (Map.Entry<GOperator, DetLimitSlave.State> entry2 : productState.getSecondaryMap().entrySet()) {
                        if (succ.isAccepting(valuation)) {
                            accSet.add(Util.getId(infSetMapping, entry2.getKey()) + 1);
                        }
                    }

                    for (int i = sizeSec + 1; i < maxAcc; i++) {
                        accSet.add(i);
                    }

                    consumer.addEdgeWithLabel(Util.getId(stateIDs, productState), edgeLabel, Collections.singletonList(Util.getId(stateIDs, succ)), accSet);
                }
            }
        }
    }

    private class Converter implements Visitor<BooleanExpression<AtomLabel>> {
        @Override
        public BooleanExpression<AtomLabel> defaultAction(Formula f) {
            throw new IllegalArgumentException("Cannot convert " + f + " to BooleanExpression.");
        }

        @Override
        public BooleanExpression<AtomLabel> visit(BooleanConstant b) {
            return new BooleanExpression<>(b.value);
        }

        @Override
        public BooleanExpression<AtomLabel> visit(Conjunction c) {
            return c.getChildren().stream().map(e -> e.accept(this)).reduce(new BooleanExpression<>(true), (e1, e2) -> e1.and(e2));
        }

        @Override
        public BooleanExpression<AtomLabel> visit(Disjunction d) {
            return d.getChildren().stream().map(e -> e.accept(this)).reduce(new BooleanExpression<>(false), (e1, e2) -> e1.or(e2));
        }

        @Override
        public BooleanExpression<AtomLabel> visit(Literal l) {
            BooleanExpression<AtomLabel> atom = new BooleanExpression<>(AtomLabel.createAlias(l.getAtom()));

            if (l.getNegated()) {
                atom = atom.not();
            }

            return atom;
        }
    }
}