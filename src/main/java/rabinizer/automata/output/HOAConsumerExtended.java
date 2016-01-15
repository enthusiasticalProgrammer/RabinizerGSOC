package rabinizer.automata.output;

import com.google.common.collect.ImmutableList;
import jhoafparser.ast.AtomAcceptance;
import jhoafparser.ast.AtomLabel;
import jhoafparser.ast.BooleanExpression;
import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import rabinizer.automata.GRabinPair;
import rabinizer.ltl.Conjunction;
import rabinizer.ltl.Formula;
import rabinizer.ltl.Literal;
import rabinizer.ltl.Simplifier;

import java.util.*;

public class HOAConsumerExtended<T> {

    public static final BooleanExpression TRUE = new BooleanExpression<>(BooleanExpression.Type.EXP_TRUE, null, null);
    public static final BooleanExpression FALSE = new BooleanExpression<>(BooleanExpression.Type.EXP_FALSE, null, null);
    private final HOAConsumer hoa;

    private final Map<T, Integer> stateNumbers;
    private final Map<Object, Integer> acceptanceNumbers;

    private AutomatonType accType;
    private boolean body;
    private List<String> alphabet;

    public HOAConsumerExtended(HOAConsumer hoa, AutomatonType type) {
        this.hoa = hoa;
        stateNumbers = new HashMap<>();
        acceptanceNumbers = new HashMap<>();
        accType = type;
    }

    private static AccType getAccCondition(List<GRabinPair<?>> acc) {
        if (acc.isEmpty()) {
            return AccType.NONE;
        }

        if (acc.size() == 1) {
            if (acc.get(0).left == null) {
                return AccType.COBUCHI;
            }

            if (acc.get(0).right == null) {
                return AccType.BUCHI;
            }
        }

        if (acc.stream().allMatch(pair -> pair.left == null)) {
            return AccType.GENBUCHI;
        }

        if (acc.stream().allMatch(pair -> pair.right == null || pair.right.size() <= 1)) {
            return AccType.RABIN;
        }

        return AccType.GENRABIN;
    }

    /**
     * this sets the version, the tool (Rabinizer), and the atomic propositions
     *
     * @throws HOAConsumerException
     */
    public void setHeader(Formula formula, Collection<String> APs) throws HOAConsumerException {
        hoa.notifyHeaderStart("v1");
        hoa.setTool("Rabinizer", "infty");
        hoa.setName("Automaton for " + formula);

        alphabet = ImmutableList.copyOf(APs);
        hoa.setAPs(alphabet);
        for (String letter : APs) {
            hoa.addAlias(letter, new BooleanExpression<>(AtomLabel.createAPIndex(alphabet.indexOf(letter))));
        }
    }

    /**
     * @throws HOAConsumerException
     */
    public void setInitialState(T initialState) throws HOAConsumerException {
        if (stateNumbers.containsKey(initialState)) {
            hoa.addStartStates(Arrays.asList(stateNumbers.get(initialState)));
        } else {
            stateNumbers.put(initialState, stateNumbers.keySet().size());
            hoa.addStartStates(Arrays.asList(stateNumbers.get(initialState)));
        }
    }

    /**
     * Checks if the acceptanceCondition is generalisedRabin or if it can be
     * specified more precise, for example if it is coBuchi, or Buchi,
     * genaralized Buchi, or Rabin.
     *
     * @throws HOAConsumerException
     */
    public void setAcceptanceCondition(List<GRabinPair<?>> acc) throws HOAConsumerException {
        AccType accT = getAccCondition(acc);
        hoa.provideAcceptanceName(accT.toString(), Collections.emptyList());
        setAccCond(acc);
    }

    public void setBuchiAcceptance() throws HOAConsumerException {
        hoa.provideAcceptanceName(AccType.BUCHI.toString(), Collections.emptyList());
        hoa.setAcceptanceCondition(1, new BooleanExpression<>(mkInf(0)));
    }

    public void setGenBuchiAcceptance(int i) throws HOAConsumerException {
        hoa.provideAcceptanceName(AccType.GENBUCHI.toString(), Collections.singletonList(i));
        hoa.setAcceptanceCondition(i, mkInfAnd(i));
    }

    private static BooleanExpression<AtomAcceptance> mkInfAnd(int j) {
        BooleanExpression<AtomAcceptance> conjunction = new BooleanExpression<>(mkInf(0));

        for (int i = 1; i < j; i++) {
            conjunction = conjunction.and(new BooleanExpression<>(mkInf(i)));
        }

        return conjunction;
    }

    public void addEdge(T begin, Formula label, T end, List<Integer> accSets) throws HOAConsumerException {
        hoa.addEdgeWithLabel(stateNumbers.get(begin), Simplifier.simplify(label).accept(new FormulaConverter()), Collections.singletonList(getStateId(end)), accSets);
    }

    public void addEdge(T begin, Formula label, T end) throws HOAConsumerException {
        if (accType.equals(AutomatonType.TRANSITION)) {
            throw new UnsupportedOperationException("For transition-acceptance-based automata, please use the other addEdge method, where you also put accSets");
        }

        addEdge(begin, label, end, null);
    }

    public void addEdge(T begin, Set<String> label, T end) throws HOAConsumerException {
        addEdge(begin, new Conjunction(alphabet.stream().map(l -> new Literal(l, !label.contains(l)))), end, null);
    }

    public void addState(T s) throws HOAConsumerException {
        if (accType.equals(AutomatonType.STATE)) {
            throw new UnsupportedOperationException("For state-acceptance-based automata, please use the other addState method, where you also put accSets");
        }

        addState(s, null);
    }

    T currentState;

    public void addState(T s, List<Integer> accSets) throws HOAConsumerException {
        if (!body) {
            hoa.notifyBodyStart();
            body = true;
        }

        currentState = s;

        hoa.addState(getStateId(s), s.toString(), null, accSets);
    }

    public Integer getNumber(Object o) {
        if (acceptanceNumbers.containsKey(o)) {
            return acceptanceNumbers.get(o);
        } else {
            throw new IllegalArgumentException("acceptance-set is not yet stored");
        }
    }

    public void done() throws HOAConsumerException {
        hoa.notifyEnd();
    }

    public void stateDone() throws HOAConsumerException {
        hoa.notifyEndOfState(getStateId(currentState));
        currentState = null;
    }

    private void setAccCond(List<GRabinPair<?>> acc) throws HOAConsumerException {
        BooleanExpression<AtomAcceptance> all = new BooleanExpression<>(BooleanExpression.Type.EXP_FALSE, null, null);

        int currentVal = 0;
        for (GRabinPair<?> rabin : acc) {
            BooleanExpression<AtomAcceptance> left = TRUE;
            BooleanExpression<AtomAcceptance> right = TRUE;
            BooleanExpression<AtomAcceptance> both;

            if (rabin.left != null) {
                left = new BooleanExpression<>(mkFin(currentVal));
                acceptanceNumbers.put(rabin.left, currentVal++);
            }


            if (rabin.right != null) {
                for (Object inf : rabin.right) {
                    right = new BooleanExpression<>(BooleanExpression.Type.EXP_AND, right,
                            new BooleanExpression<>(mkInf(currentVal)));
                    acceptanceNumbers.put(inf, currentVal++);
                }
            }

            both = new BooleanExpression<AtomAcceptance>(BooleanExpression.Type.EXP_AND, left, right);
            all = new BooleanExpression<>(BooleanExpression.Type.EXP_OR, all, both);
        }

        hoa.setAcceptanceCondition(currentVal, new RemoveConstants().visit(all));
    }

    private int getStateId(T state) {
        if (!stateNumbers.containsKey(state)) {
            stateNumbers.put(state, stateNumbers.keySet().size());
        }

        return stateNumbers.get(state);
    }

    private static AtomAcceptance mkInf(int i) {
        return new AtomAcceptance(AtomAcceptance.Type.TEMPORAL_INF, i, false);
    }

    private static AtomAcceptance mkFin(int i) {
        return new AtomAcceptance(AtomAcceptance.Type.TEMPORAL_FIN, i, false);
    }

    public enum AutomatonType {
        STATE, TRANSITION;
    }

    public enum AccType {
        NONE, ALL, BUCHI, COBUCHI, GENBUCHI, RABIN, GENRABIN;

        @Override
        public String toString() {
            switch (this) {
                case NONE:
                    return "none";

                case ALL:
                    return "all";

                case BUCHI:
                    return "Buchi";

                case COBUCHI:
                    return "co-Buchi";

                case GENBUCHI:
                    return "generalized-Buchi";

                case RABIN:
                    return "Rabin";

                case GENRABIN:
                    return "generalized-Rabin";

                default:
                    throw new AssertionError();
            }
        }
    }
}
