package rabinizer.automata;

import org.jetbrains.annotations.NotNull;
import rabinizer.exec.Main;
import rabinizer.collections.valuationset.ValuationSet;
import rabinizer.collections.valuationset.ValuationSetFactory;

import java.util.*;

/**
 * @author jkretinsky
 */
public class RabinSlave extends Automaton<RabinSlave.State> {

    public MojmirSlave mojmir;

    public RabinSlave(MojmirSlave mojmir, ValuationSetFactory factory) {
        super(factory, false);
        this.mojmir = mojmir;

        MojmirSlave.State f = mojmir.trapState;
        State trap = new State();
        trap.put(f, 1);
        trapState = trap;
    }

    public void optimizeInitialState() { // TODO better: reach BSCC
        while (noIncomingTransitions(initialState) && !transitions.row(initialState).isEmpty()) {
            Main.verboseln("Optimizing initial states");
            State oldInit = initialState;
            initialState = getSuccessor(initialState, Collections.emptySet());
            transitions.row(oldInit).clear();
            states.remove(oldInit);
        }
    }

    @Override
    protected @NotNull State generateInitialState() {
        State init = new State();
        init.put(mojmir.getInitialState(), 1);
        return init;
    }

    private boolean noIncomingTransitions(RabinSlave.State in) {
        return !transitions.values().contains(in);
    }

    public class State extends HashMap<MojmirSlave.State, Integer> implements IState<State> {
        private static final long serialVersionUID = 1L;

        @Override
        public String toString() {
            String result = "";
            for (MojmirSlave.State f : keySet()) {
                result += " " + f + "=" + get(f);
            }
            return result;
        }

        @Override
        public Set<ValuationSet> partitionSuccessors() {
            Set<Set<ValuationSet>> product = new HashSet<>();
            for (MojmirSlave.State fs : keySet()) {
                product.add(mojmir.transitions.row(fs).keySet());
            }
            return generatePartitioning(product);
        }

        @Override
        public Set<String> getSensitiveAlphabet() {
            return valuationSetFactory.getAlphabet();
        }

        @Override
        public ValuationSetFactory getFactory() {
            return valuationSetFactory;
        }

        @Override
        public State getSuccessor(@NotNull Set<String> valuation) {
            State succ = new State();

            // move tokens, keeping the lowest only
            for (MojmirSlave.State currFormula : keySet()) {
                MojmirSlave.State succFormula = currFormula.getSuccessor(valuation);
                if (succ.get(succFormula) == null || succ.get(succFormula) > get(currFormula)) {
                    succ.put(succFormula, get(currFormula));
                }
            }

            mojmir.states.stream().filter(mojmir::isSink).forEach(succ::remove);

            // TODO recompute tokens, eliminating gaps
            int[] tokens = new int[succ.keySet().size()];
            int i = 0;
            for (Entry<MojmirSlave.State, Integer> stateIntegerEntry : succ.entrySet()) {
                tokens[i] = stateIntegerEntry.getValue();
                i++;
            }
            Arrays.sort(tokens);
            for (Entry<MojmirSlave.State, Integer> stateIntegerEntry : succ.entrySet()) {
                for (int j = 0; j < tokens.length; j++) {
                    if (stateIntegerEntry.getValue().equals(tokens[j])) {
                        succ.put(stateIntegerEntry.getKey(), j + 1);
                    }
                }
            }

            // TODO add token to the initial state
            if (!succ.containsKey(mojmir.getInitialState())) {
                succ.put(mojmir.getInitialState(), succ.keySet().size() + 1);
            }

            return succ;
        }
    }
}
