package rabinizer.automata.nxt;

import com.google.common.collect.Sets;
import jhoafparser.ast.AtomAcceptance;
import jhoafparser.ast.AtomLabel;
import jhoafparser.ast.BooleanExpression;
import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import rabinizer.automata.Master;
import rabinizer.automata.Optimisation;
import rabinizer.automata.SCCAnalyser;
import rabinizer.ltl.*;
import rabinizer.ltl.bdd.BDDEquivalenceClassFactory;
import rabinizer.ltl.bdd.BDDValuationSetFactory;

import java.util.*;

public class DetLimitAutomaton {

    protected final Formula initialFormula;
    protected final EquivalenceClassFactory equivalenceClassFactory;
    protected final ValuationSetFactory valuationSetFactory;

    protected final DetComponent detComponent;
    protected final NonDetComponent nonDetComponent;

    protected final int accSize;
    protected final Set<Master.State> initialStates;

    public DetLimitAutomaton(Formula formula, Collection<Optimisation> optimisations) {
        initialFormula = formula;

        Set<Formula> initialFormulas;

        if (optimisations.contains(Optimisation.OR_BREAKUP) && formula instanceof Disjunction) {
            Disjunction dis = (Disjunction) formula;
            initialFormulas = dis.getChildren();
        } else {
            initialFormulas = Collections.singleton(formula);
        }

        Set<Formula> props = new HashSet<>();

        for (Set<GOperator> gSet : Sets.powerSet(formula.gSubformulas())) {
            Visitor<Formula> masterVisitor = new GSubstitutionVisitor(g -> gSet.contains(g) ? null : BooleanConstant.FALSE);
            Visitor<Formula> masterVisitor2 = new GSubstitutionVisitor(g -> gSet.contains(g) ? BooleanConstant.TRUE : BooleanConstant.FALSE);


            addAllCheckNegation(props, Simplifier.simplify(formula.accept(masterVisitor)).getPropositions());
            addAllCheckNegation(props, Simplifier.simplify(formula.accept(masterVisitor2)).getPropositions());
        }

        Set<Formula> hotfix = new HashSet<>();

        for (Formula prop : props) {
            if (prop instanceof UOperator) {
                UOperator op = (UOperator) prop;
                hotfix.add(new FOperator(op.right));
            }
        }

        addAllCheckNegation(props, hotfix);

        equivalenceClassFactory = new BDDEquivalenceClassFactory(props);
        valuationSetFactory = new BDDValuationSetFactory(initialFormula.getAtoms());

        nonDetComponent = new NonDetComponent(equivalenceClassFactory, valuationSetFactory, optimisations);
        detComponent = new DetComponent(nonDetComponent, equivalenceClassFactory, valuationSetFactory, optimisations);

        initialStates = new HashSet<>();

        int accSizeCounter = 1;

        for (Formula initialFormula : initialFormulas) {
            Set<Set<GOperator>> keys = optimisations.contains(Optimisation.SKELETON) ? computeSkeletonKeys(initialFormula) : Sets.powerSet(initialFormula.gSubformulas());



            for (Set<GOperator> key : keys) {
                // TODO: Fix substitition bug.

                accSizeCounter = Math.max(accSizeCounter, key.size() + 1);

                Visitor<Formula> masterVisitor = new GSubstitutionVisitor(g -> key.contains(g) ? null : BooleanConstant.FALSE);
                EquivalenceClass initial = equivalenceClassFactory.createEquivalenceClass(Simplifier.simplify(initialFormula.accept(masterVisitor)));

                if (!initial.isFalse()) {
                    Master.State initialState = nonDetComponent.generateInitialState(initial);
                    nonDetComponent.generate(initialState);
                    initialStates.add(initialState);
                }
            }
        }

        accSize = accSizeCounter;
    }

    private static Set<Set<GOperator>> computeSkeletonKeys(Formula formula) {
        EquivalenceClassFactory factory = new BDDEquivalenceClassFactory(formula.getPropositions());
        Formula skeleton = formula.accept(new SkeletonVisitor());
        EquivalenceClass skeletonClazz = factory.createEquivalenceClass(skeleton);

        Set<Set<GOperator>> keys = new HashSet<>();

        for (Set<GOperator> key : Sets.powerSet(formula.gSubformulas())) {
            EquivalenceClass keyClazz = factory.createEquivalenceClass(new Conjunction(key));

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

        consumer.provideAcceptanceName("generalized-Buchi", Collections.singletonList(getAcceptingSetSize()));
        consumer.setAcceptanceCondition(getAcceptingSetSize(), mkInfAnd(getAcceptingSetSize()));

        List<String> alphabetList = toList(valuationSetFactory.getAlphabet());

        consumer.setAPs(alphabetList);
        for (String letter : alphabetList) {
            consumer.addAlias(letter, new BooleanExpression<>(AtomLabel.createAPIndex(alphabetList.indexOf(letter))));
        }

        consumer.notifyBodyStart();

        nonDetComponent.toHOA(consumer, ids);
        detComponent.toHOA(consumer, ids, accSize);

        consumer.notifyEnd();
    }

    private void addAllCheckNegation(Set<Formula> set, Set<Formula> newElements) {
        for (Formula element : newElements) {
            if (!set.contains(element.not())) {
                set.add(element);
            }
        }
    }

    private int getAcceptingSetSize() {
        // TODO: fix...
        return accSize;
    }

    class NonDetComponent extends DetLimitMaster {
        final private boolean delayedJump;

        NonDetComponent(EquivalenceClassFactory equivalenceClassFactory, ValuationSetFactory valuationSetFactory, Collection<Optimisation> optimisations) {
            super(BooleanConstant.TRUE, equivalenceClassFactory, valuationSetFactory, optimisations, true);
            generate();
            delayedJump = optimisations.contains(Optimisation.DELAYED_JUMP);
        }

        public int size() {
            return states.size();
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
}


