package rabinizer.automata.buchi;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableSet;
import rabinizer.collections.Tuple;

import java.util.*;

public class SemiDeterminization {

    private final BuchiAutomaton automaton;
    private final BuchiAutomaton semi;
    private final BiMap<Tuple<Set<BuchiAutomaton.State>, Set<BuchiAutomaton.State>>, BuchiAutomaton.State> productStateMapping;

    public SemiDeterminization(BuchiAutomaton automaton) {
        this.automaton = automaton;
        this.semi = new BuchiAutomaton(automaton);
        this.semi.acceptingStates.clear();
        this.productStateMapping = HashBiMap.create();
    }

    public BuchiAutomaton apply() {
        Set<BuchiAutomaton.State> done = new HashSet<>();
        Deque<BuchiAutomaton.State> workList = new ArrayDeque<>();

        // Compute Transitions
        for (BuchiAutomaton.State f : automaton.acceptingStates) {
            for (Set<String> valuation : automaton.valuationSetFactory.createUniverseValuationSet()) {
                BuchiAutomaton.State succ = getSuccessor(new Tuple<>(Collections.singleton(f), Collections.singleton(f)), valuation);

                if (succ != null) {
                    semi.addTransition(f, valuation, succ);
                    workList.add(succ);
                }
            }

            while (!workList.isEmpty()) {
                BuchiAutomaton.State state = workList.remove();

                if (!done.contains(state)) {
                    Tuple<Set<BuchiAutomaton.State>, Set<BuchiAutomaton.State>> tuple = productStateMapping.inverse().get(state);

                    for (Set<String> valuation : automaton.valuationSetFactory.createUniverseValuationSet()) {
                        BuchiAutomaton.State successor = getSuccessor(tuple, valuation);

                        if (successor != null) {
                            semi.addTransition(state, valuation, successor);
                            workList.add(successor);
                        }
                    }

                    done.add(state);
                }
            }
        }

        // Compute Accepting States
        for (Map.Entry<Tuple<Set<BuchiAutomaton.State>, Set<BuchiAutomaton.State>>, BuchiAutomaton.State> entry : productStateMapping.entrySet()) {
            if (entry.getKey().left.stream().anyMatch(automaton.acceptingStates::contains)
                    && entry.getKey().left.equals(entry.getKey().right)) {
                semi.setAccepting(entry.getValue());
            }
        }

        return semi;
    }

    private BuchiAutomaton.State getSuccessor(Tuple<Set<BuchiAutomaton.State>, Set<BuchiAutomaton.State>> tuple, final Set<String> valuation) {
        // Standard Subset Construction
        Set<BuchiAutomaton.State> rightSuccessor = new HashSet<>();

        for (BuchiAutomaton.State rightState : tuple.right) {
            rightSuccessor.addAll(automaton.getTransitions(rightState, valuation));
        }

        Set<BuchiAutomaton.State> leftSuccessor = new HashSet<>();

        // Add all states reached from an accepting state
        tuple.right.stream()
                .filter(automaton.acceptingStates::contains)
                .forEach(s -> leftSuccessor.addAll(automaton.getTransitions(s, valuation)));

        if (!tuple.left.equals(tuple.right)) {
            tuple.left.forEach(s -> leftSuccessor.addAll(automaton.getTransitions(s, valuation)));
        }

        // Don't construct the trap state.
        if (leftSuccessor.isEmpty() && rightSuccessor.isEmpty()) {
            return null;
        }

        Tuple<Set<BuchiAutomaton.State>, Set<BuchiAutomaton.State>> prod = new Tuple<>(ImmutableSet.copyOf(leftSuccessor), ImmutableSet.copyOf(rightSuccessor));

        BuchiAutomaton.State succ = productStateMapping.get(prod);

        if (succ == null) {
            succ = semi.createState(prod.toString());
            productStateMapping.put(prod, succ);
        }

        return succ;
    }
}
