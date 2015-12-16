package rabinizer.automata;

import rabinizer.ltl.ValuationSet;

import java.util.Set;

public interface IState<S> {

    /**
     * @param valuation
     * @return null is returned if the transition would move to a non-accepting BSCC.
     */
    S getSuccessor(Set<String> valuation);

    boolean isAccepting(Set<String> valuation);

    Set<ValuationSet> partitionSuccessors();
}
