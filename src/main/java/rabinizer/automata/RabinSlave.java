package rabinizer.automata;

import rabinizer.exec.Main;
import rabinizer.ltl.EquivalenceClass;
import rabinizer.ltl.GOperator;
import rabinizer.ltl.ValuationSet;
import rabinizer.ltl.ValuationSetFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author jkretinsky
 */
public class RabinSlave extends Automaton<RankingState> {

    public FormulaAutomaton<GOperator> mojmir;

    public RabinSlave(FormulaAutomaton<GOperator> mojmir, ValuationSetFactory<String> factory) {
        super(factory);
        this.mojmir = mojmir;

        FormulaAutomatonState f = mojmir.trapState;
        RankingState trap = new RankingState();
        trap.put(f, 1);
        trapState = trap;
    }

    @Override
    protected RankingState generateInitialState() {
        RankingState init = new RankingState();
        init.put(mojmir.initialState, 1);
        return init;
    }

    @Override
    protected RankingState generateSuccState(RankingState curr, ValuationSet vs) {
        Set<String> val = vs.pickAny();
        RankingState succ = new RankingState();

        // move tokens, keeping the lowest only
        for (FormulaAutomatonState currFormula : curr.keySet()) {
            FormulaAutomatonState succFormula = mojmir.succ(currFormula, val);
            if ((succ.get(succFormula) == null) || (succ.get(succFormula) > curr.get(currFormula))) {
                succ.put(succFormula, curr.get(currFormula));
            }
        }
        for (FormulaAutomatonState s : mojmir.sinks) {
            succ.remove(s);
        }

        // TODO recompute tokens, eliminating gaps
        int[] tokens = new int[succ.keySet().size()];
        int i = 0;
        for (FormulaAutomatonState f : succ.keySet()) {
            tokens[i] = succ.get(f);
            i++;
        }
        Arrays.sort(tokens);
        for (FormulaAutomatonState f : succ.keySet()) {
            for (int j = 0; j < tokens.length; j++) {
                if (succ.get(f).equals(tokens[j])) {
                    succ.put(f, j + 1);
                }
            }
        }

        // TODO add token to the initial state
        if (!succ.containsKey(mojmir.initialState)) {
            succ.put(mojmir.initialState, succ.keySet().size() + 1);
        }

        return succ;
    }

    @Override
    protected Set<ValuationSet> generateSuccTransitions(RankingState s) {
        Set<Set<ValuationSet>> product = new HashSet<>();
        for (FormulaAutomatonState fs : s.keySet()) {
            product.add(mojmir.transitions.row(fs).keySet());
        }
        return generatePartitioning(product);
    }

    public RabinSlave optimizeInitialState() { // TODO better: reach BSCC
        while (noIncomingTransitions(initialState) && !transitions.row(initialState).isEmpty()) {
            Main.verboseln("Optimizing initial states");
            RankingState oldInit = initialState;
            initialState = succ(initialState, Collections.emptySet());
            transitions.row(oldInit).clear();
            states.remove(oldInit);
        }
        return this;
    }

    private boolean noIncomingTransitions(RankingState in) {
        return !transitions.values().contains(in);
    }

}
