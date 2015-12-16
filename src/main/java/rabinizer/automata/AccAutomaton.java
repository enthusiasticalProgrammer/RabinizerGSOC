package rabinizer.automata;

import rabinizer.ltl.ValuationSetFactory;

import java.util.Map;

/**
 * @author jkretinsky
 */
public abstract class AccAutomaton<S extends IState<S>> extends Automaton<S> {

    protected AccAutomaton(ValuationSetFactory<String> valuationSetFactory) {
        super(valuationSetFactory);
    }

    @Override
    protected abstract String accTypeNumerical();

    @Override
    protected abstract String stateAcc(S s);

    @Override
    protected abstract String outTransToHOA(S s, Map<S, Integer> statesToNumbers);

}
