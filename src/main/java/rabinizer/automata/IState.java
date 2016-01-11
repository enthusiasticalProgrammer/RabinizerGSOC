package rabinizer.automata;

import com.google.common.collect.Sets;
import rabinizer.ltl.ValuationSet;
import rabinizer.ltl.ValuationSetFactory;

import java.util.*;
import java.util.stream.Collectors;

public interface IState<S> {

    /**
     * @param valuation
     * @return null is returned if the transition would move to a non-accepting BSCC.
     */
    S getSuccessor(Set<String> valuation);

    default Map<ValuationSet, S> getSuccessors() {
        Map<ValuationSet,S> successors = new HashMap<>();

        for (ValuationSet valuationSet : partitionSuccessors()) {
            S successor = getSuccessor(valuationSet.pickAny());

            if (successor != null) {
                successors.put(valuationSet, successor);
            }
        }

        return successors;
    }

    default Set<ValuationSet> partitionSuccessors() {
        Set<String> sensitiveAlphabet = getSensitiveAlphabet();
        ValuationSetFactory factory = getFactory();
        return Sets.powerSet(sensitiveAlphabet).stream().map(subset -> factory.createValuationSet(subset, sensitiveAlphabet)).collect(Collectors.toSet());
    }

    Set<String> getSensitiveAlphabet();

    ValuationSetFactory getFactory();
}
