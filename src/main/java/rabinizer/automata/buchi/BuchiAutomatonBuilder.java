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

package rabinizer.automata.buchi;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import jhoafparser.ast.AtomAcceptance;
import jhoafparser.ast.AtomLabel;
import jhoafparser.ast.BooleanExpression;
import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import rabinizer.automata.buchi.BuchiAutomaton.State;
import rabinizer.automata.output.HOAConsumerExtended;
import ltl.Collections3;
import rabinizer.collections.valuationset.BDDValuationSetFactory;
import rabinizer.collections.valuationset.ValuationSet;
import rabinizer.collections.valuationset.ValuationSetFactory;
import ltl.BooleanConstant;

import java.util.*;

public class BuchiAutomatonBuilder implements HOAConsumer {

    private final Deque<BuchiAutomaton> automata = new ArrayDeque<>();
    private BuchiAutomaton automaton;
    private ValuationSetFactory valuationSetFactory;
    private Integer initialState;
    private State[] integerToState;
    private int implicitEdgeCounter;
    private HOAConsumerExtended.AccType accType;

    @Override
    public boolean parserResolvesAliases() {
        return false;
    }

    @Override
    public void notifyHeaderStart(String s) {
        valuationSetFactory = null;
        integerToState = null;
        initialState = null;
        automaton = null;
    }

    @Override
    public void setNumberOfStates(int i) throws HOAConsumerException {
        integerToState = new State[i];
    }

    @Override
    public void addStartStates(List<Integer> list) throws HOAConsumerException {
        if (list.size() != 1 || initialState != null) {
            throw new HOAConsumerException("Only a single initial state is supported.");
        }

        initialState = list.get(0);
    }

    @Override
    public void addAlias(String s, BooleanExpression<AtomLabel> booleanExpression) throws HOAConsumerException {
        throw new HOAConsumerException("Unsupported Operation.");
    }

    @Override
    public void setAPs(List<String> list) throws HOAConsumerException {
        BiMap<String, Integer> aliases = HashBiMap.create(list.size());
        list.forEach(ap -> aliases.put(ap, aliases.size()));
        valuationSetFactory = new BDDValuationSetFactory(list.size(), aliases);
    }

    @Override
    public void setAcceptanceCondition(int i, BooleanExpression<AtomAcceptance> booleanExpression) throws HOAConsumerException {
        if (i == 0 && (accType == HOAConsumerExtended.AccType.NONE || accType == HOAConsumerExtended.AccType.ALL)) {
            return;
        }

        if (i == 1 && accType == HOAConsumerExtended.AccType.BUCHI) {
            return;
        }

        throw new HOAConsumerException("Unsupported Acceptance Conditions: " + i + ' ' + booleanExpression);
    }

    @Override
    public void provideAcceptanceName(String s, List<Object> list) throws HOAConsumerException {
        switch (s) {
            case "all":
                accType = HOAConsumerExtended.AccType.ALL;
                break;

            case "none":
                accType = HOAConsumerExtended.AccType.NONE;
                break;

            case "Buchi":
                accType = HOAConsumerExtended.AccType.BUCHI;
                break;

            default:
                throw new HOAConsumerException("Unsupported Acceptance Name: " + s);
        }
    }

    @Override
    public void setName(String s) throws HOAConsumerException {
        // No operation
    }

    @Override
    public void setTool(String s, String s1) throws HOAConsumerException {
        // No operation
    }

    @Override
    public void addProperties(List<String> list) throws HOAConsumerException {
        // No operation
    }

    @Override
    public void addMiscHeader(String s, List<Object> list) throws HOAConsumerException {
        // No operation
    }

    @Override
    public void notifyBodyStart() throws HOAConsumerException {
        if (valuationSetFactory == null) {
            valuationSetFactory = new BDDValuationSetFactory(BooleanConstant.TRUE, ImmutableBiMap.of());
        }

        automaton = new BuchiAutomaton(valuationSetFactory);

        if (accType == HOAConsumerExtended.AccType.ALL || accType == HOAConsumerExtended.AccType.NONE) {
            for (BitSet valuation : Collections3.powerSet(valuationSetFactory.getSize())) {
                automaton.addTransition(automaton.getInitialState(), valuation, automaton.getInitialState());
            }
        }

        // Fixme: NPE
        automaton.getInitialState().label = Integer.toString(initialState);

        if (accType == HOAConsumerExtended.AccType.ALL) {
            automaton.setAccepting(automaton.getInitialState());
        } else if (accType == HOAConsumerExtended.AccType.BUCHI) {
            ensureSpaceInMap(initialState);
            integerToState[initialState] = automaton.getInitialState();
        }
    }

    @Override
    public void addState(int i, String s, BooleanExpression<AtomLabel> booleanExpression, List<Integer> list) throws HOAConsumerException {
        if (accType == HOAConsumerExtended.AccType.ALL || accType == HOAConsumerExtended.AccType.NONE) {
            return;
        }

        State state = addState(s, i);

        if (list != null) {
            if (list.size() > 1) {
                throw new HOAConsumerException("Unsupported acceptance");
            }

            if (list.contains(0)) {
                automaton.setAccepting(state);
            }
        }
    }

    @Override
    public void addEdgeImplicit(int i, List<Integer> list, List<Integer> list1) throws HOAConsumerException {
        if (accType == HOAConsumerExtended.AccType.ALL || accType == HOAConsumerExtended.AccType.NONE) {
            return;
        }

        addEdgeWithLabel(i, BooleanExpression.fromImplicit(implicitEdgeCounter, valuationSetFactory.getSize()), list, list1);
        implicitEdgeCounter++;
    }

    @Override
    public void addEdgeWithLabel(int i, BooleanExpression<AtomLabel> booleanExpression, List<Integer> list, List<Integer> list1) throws HOAConsumerException {
        if (accType == HOAConsumerExtended.AccType.ALL || accType == HOAConsumerExtended.AccType.NONE) {
            return;
        }

        State source = integerToState[i];

        if (source == null) {
            source = addState(null, i);
        }

        if (list1 != null && !list1.isEmpty()) {
            throw new HOAConsumerException("Edge acceptance not supported");
        }

        if (list.size() > 1) {
            throw new HOAConsumerException("Unsupported transitions targets");
        }

        if (!list.isEmpty()) {
            State target = integerToState[list.get(0)];

            if (target == null) {
                target = addState(null, list.get(0));
            }

            ValuationSet valuationSet = toValuationSet(booleanExpression);

            for (BitSet valuation : valuationSet) {
                automaton.addTransition(source, valuation, target);
            }
        }
    }

    @Override
    public void notifyEndOfState(int i) throws HOAConsumerException {
        implicitEdgeCounter = 0;
    }

    @Override
    public void notifyEnd() throws HOAConsumerException {
        automata.add(automaton);
        notifyHeaderStart(null);
    }

    @Override
    public void notifyAbort() {
        notifyHeaderStart(null);
    }

    @Override
    public void notifyWarning(String s) throws HOAConsumerException {
        // No operation
    }

    public List<BuchiAutomaton> getAutomata() {
        return new ArrayList<>(automata);
    }

    private void ensureSpaceInMap(int id) {
        if (integerToState == null) {
            integerToState = new State[id + 1];
        }

        if (id >= integerToState.length) {
            integerToState = Arrays.copyOf(integerToState, id + 1);
        }
    }

    private State addState(String label, int id) {
        ensureSpaceInMap(id);

        if (integerToState[id] == null) {
            integerToState[id] = automaton.createState(label == null ? Integer.toString(id) : label);
        }

        if (integerToState[id].label == null) {
            integerToState[id].label = label;
        }

        return integerToState[id];
    }

    private ValuationSet toValuationSet(BooleanExpression<AtomLabel> label) {
        if (label.isFALSE()) {
            return valuationSetFactory.createEmptyValuationSet();
        }

        if (label.isTRUE()) {
            return valuationSetFactory.createUniverseValuationSet();
        }

        if (label.isAtom()) {
            BitSet bs = new BitSet();
            bs.set(label.getAtom().getAPIndex());
            return valuationSetFactory.createValuationSet(bs, bs);
        }

        if (label.isNOT()) {
            return toValuationSet(label.getLeft()).complement();
        }

        if (label.isAND()) {
            ValuationSet valuationSet = toValuationSet(label.getLeft());
            valuationSet.retainAll(toValuationSet(label.getRight()));
            return valuationSet;
        }

        if (label.isOR()) {
            ValuationSet valuationSet = toValuationSet(label.getLeft());
            valuationSet.addAll(toValuationSet(label.getRight()));
            return valuationSet;
        }

        throw new IllegalArgumentException("Unsupported Case: " + label);
    }
}
