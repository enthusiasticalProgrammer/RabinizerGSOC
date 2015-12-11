package rabinizer.automata;

import rabinizer.ltl.ValuationSetFactory;

import java.util.Map;

/**
 * @author jkretinsky
 */
public abstract class AccAutomaton<State> extends Automaton<State> {

    protected AccAutomaton(ValuationSetFactory<String> valuationSetFactory) {
        super(valuationSetFactory);
    }

    @Override
    protected abstract String accTypeNumerical();

    @Override
    protected abstract String stateAcc(State s);

    @Override
    protected abstract String outTransToHOA(State s, Map<State, Integer> statesToNumbers);

}
