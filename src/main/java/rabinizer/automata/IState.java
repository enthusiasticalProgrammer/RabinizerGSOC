package rabinizer.automata;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rabinizer.collections.valuationset.ValuationSet;
import rabinizer.collections.valuationset.ValuationSetFactory;

import java.util.*;
import java.util.stream.Collectors;

public interface IState<S> {

    /**
     * @param valuation
     * @return null is returned if the transition would move to a non-accepting BSCC.
     */
    @Nullable S getSuccessor(@NotNull Set<String> valuation);

    default @NotNull Map<ValuationSet, S> getSuccessors() {
        Map<ValuationSet,S> successors = new HashMap<>();

        for (ValuationSet valuationSet : partitionSuccessors()) {
            S successor = getSuccessor(valuationSet.pickAny());

            if (successor != null) {
                successors.put(valuationSet, successor);
            }
        }

        return successors;
    }

    default @NotNull Set<ValuationSet> partitionSuccessors() {
        Set<String> sensitiveAlphabet = getSensitiveAlphabet();
        ValuationSetFactory factory = getFactory();
        return Sets.powerSet(sensitiveAlphabet).stream().map(subset -> factory.createValuationSet(subset, sensitiveAlphabet)).collect(Collectors.toSet());
    }

    @NotNull Set<String> getSensitiveAlphabet();

    @NotNull ValuationSetFactory getFactory();
}
