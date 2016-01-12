package rabinizer.automata;

import rabinizer.ltl.ValuationSetFactory;

/**
 * @author jkretinsky
 */
public abstract class AccAutomaton<S extends IState<S>> extends Automaton<S> {

    protected AccAutomaton(ValuationSetFactory valuationSetFactory) {
        super(valuationSetFactory);
    }

}
