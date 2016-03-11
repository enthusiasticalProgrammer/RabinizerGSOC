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

package rabinizer.automata.output;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import jhoafparser.ast.AtomAcceptance;
import jhoafparser.ast.AtomLabel;
import jhoafparser.ast.BooleanExpression;
import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import rabinizer.automata.*;
import rabinizer.collections.Collections3;
import rabinizer.collections.valuationset.ValuationSet;
import rabinizer.ltl.Conjunction;
import rabinizer.ltl.Formula;
import rabinizer.ltl.Literal;
import rabinizer.ltl.simplifier.Simplifier;

import java.util.*;
import java.util.stream.Collectors;

public class HOAConsumerExtended<T> {

    public static final BooleanExpression<AtomAcceptance> TRUE = new BooleanExpression<>(BooleanExpression.Type.EXP_TRUE, null, null);
    private final HOAConsumer hoa;

    private final Map<T, Integer> stateNumbers;
    private final Map<Object, Integer> acceptanceNumbers;

    private final AutomatonType accType;
    private boolean body;
    private List<String> alphabet;

    public HOAConsumerExtended(HOAConsumer hoa, AutomatonType type) {
        this.hoa = hoa;
        stateNumbers = new HashMap<>();
        acceptanceNumbers = new HashMap<>();
        accType = type;
    }

    private static <T> AccType getAccCondition(Collection<GeneralizedRabinPair<T>> acc) {
        if (acc.isEmpty()) {
            return AccType.NONE;
        }

        if (acc.size() == 1) {
            GeneralizedRabinPair<T> pair = Collections3.getElement(acc);

            if (pair.fin.isEmpty() || pair.infs.size() == 1) {
                return AccType.BUCHI;
            }

            if (pair.infs.isEmpty()) {
                return AccType.COBUCHI;
            }
        }

        if (acc.stream().allMatch(pair -> pair.fin.isEmpty())) {
            return AccType.GENBUCHI;
        }

        if (acc.stream().allMatch(pair -> pair.infs.size() <= 1)) {
            return AccType.RABIN;
        }

        return AccType.GENRABIN;
    }

    /**
     * this sets the version, the tool (Rabinizer), and the atomic propositions
     *
     * @throws HOAConsumerException
     */
    public void setHeader(String info, Collection<String> APs) throws HOAConsumerException {
        hoa.notifyHeaderStart("v1");
        hoa.setTool("Rabinizer", "infty");
        hoa.setName("Automaton for " + info);

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
            hoa.addStartStates(Collections.singletonList(stateNumbers.get(initialState)));
        } else {
            stateNumbers.put(initialState, stateNumbers.keySet().size());
            hoa.addStartStates(Collections.singletonList(stateNumbers.get(initialState)));
        }
    }

    /**
     * Checks if the acceptanceCondition is generalisedRabin or if it can be
     * specified more precise, for example if it is coBuchi, or Buchi,
     * genaralized Buchi, or Rabin.
     *
     * @throws HOAConsumerException
     */
    public void setAcceptanceCondition(Collection<GeneralizedRabinPair<T>> acc) throws HOAConsumerException {
        AccType accT = getAccCondition(acc);

        hoa.provideAcceptanceName(accT.toString(), Collections.emptyList());
        setAccCond(acc);
    }

    public void setAcceptanceCondition2(Collection<RabinPair<T>> acc) throws HOAConsumerException {
        setAcceptanceCondition(acc.stream().map(pair -> new GeneralizedRabinPair<>(pair.fin, Collections.singletonList(pair.inf))).collect(Collectors.toList()));
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

    public void addEdges(T begin, Map<ValuationSet, ? extends T> successors) throws HOAConsumerException {
        for (Map.Entry<ValuationSet, ? extends T> entry : successors.entrySet()) {
            hoa.addEdgeWithLabel(stateNumbers.get(begin), Simplifier.simplify(entry.getKey().toFormula()).accept(new FormulaConverter()), Collections.singletonList(getStateId(entry.getValue())), null);
        }
    }

    public void addEdges2(T begin, T successor) throws HOAConsumerException {
        hoa.addEdgeWithLabel(getStateId(begin), null, Collections.singletonList(getStateId(successor)), null);
    }

    public void addEdges2(T begin, Map<ValuationSet, Set<?>> successors) throws HOAConsumerException {
        for (Map.Entry<ValuationSet, Set<?>> entry : successors.entrySet()) {
            for (Object successor : entry.getValue()) {
                hoa.addEdgeWithLabel(stateNumbers.get(begin), Simplifier.simplify(entry.getKey().toFormula()).accept(new FormulaConverter()), Collections.singletonList(getStateId((T) successor)), null);
            }
        }
    }

    public void addEdge(T begin, Formula label, T end, List<Integer> accSets) throws HOAConsumerException {
        if (accSets != null && accType == AutomatonType.STATE) {
            throw new UnsupportedOperationException("For state-acceptance-based automata, please use the other addEdge method, where you also put accSets");
        }

        hoa.addEdgeWithLabel(stateNumbers.get(begin), Simplifier.simplify(label).accept(new FormulaConverter()), Collections.singletonList(getStateId(end)), accSets);
    }

    public void addEdge(T begin, Formula label, T end) throws HOAConsumerException {
        addEdge(begin, label, end, null);
    }

    public void addEdge(T begin, Set<String> label, T end) throws HOAConsumerException {
        addEdge(begin, new Conjunction(alphabet.stream().map(l -> new Literal(l, !label.contains(l)))), end, null);
    }

    public void addState(T s) throws HOAConsumerException {
        addState(s, null);
    }

    T currentState;

    public void addState(T s, List<Integer> accSets) throws HOAConsumerException {
        if (accSets != null && accType == AutomatonType.TRANSITION) {
            throw new UnsupportedOperationException("For transition-acceptance-based automata, please use the other addState method, where you also put accSets");
        }

        if (!body) {
            hoa.notifyBodyStart();
            body = true;
        }

        currentState = s;
        hoa.addState(getStateId(s), s.toString(), null, accSets);
    }

    public Integer getNumber(Object o) {
        if (!acceptanceNumbers.containsKey(o)) {
            acceptanceNumbers.put(o, acceptanceNumbers.size());
        }

        return acceptanceNumbers.get(o);
    }

    public void done() throws HOAConsumerException {
        hoa.notifyEnd();
    }

    public void stateDone() throws HOAConsumerException {
        hoa.notifyEndOfState(getStateId(currentState));
        currentState = null;
    }


    private void setAccCond(Collection<GeneralizedRabinPair<T>> acc) throws HOAConsumerException {
        BooleanExpression<AtomAcceptance> all = new BooleanExpression<>(BooleanExpression.Type.EXP_FALSE, null, null);

        for (GeneralizedRabinPair<T> rabin : acc) {
            BooleanExpression<AtomAcceptance> left = TRUE;
            BooleanExpression<AtomAcceptance> right = TRUE;
            BooleanExpression<AtomAcceptance> both;

            if (!rabin.fin.isEmpty()) {
                left = new BooleanExpression<>(mkFin(getNumber(rabin.fin)));
            }


            if (!rabin.infs.isEmpty()) {
                for (TranSet<T> inf : rabin.infs) {
                    right = new BooleanExpression<>(BooleanExpression.Type.EXP_AND, right, new BooleanExpression<>(mkInf(getNumber(inf))));
                }
            }

            both = new BooleanExpression<>(BooleanExpression.Type.EXP_AND, left, right);
            all = new BooleanExpression<>(BooleanExpression.Type.EXP_OR, all, both);
        }

        hoa.setAcceptanceCondition(acceptanceNumbers.size(), new RemoveConstants<AtomAcceptance>().visit(all));
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
        STATE, TRANSITION
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
