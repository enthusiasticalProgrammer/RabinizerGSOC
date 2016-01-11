package rabinizer.ltl;

import java.util.Set;

public interface ValuationSetFactory {
    Set<ValuationSet> createAllValuationSets();

    ValuationSet createEmptyValuationSet();

    ValuationSet createUniverseValuationSet();

    ValuationSet createValuationSet(Set<String> valuation);

    ValuationSet createValuationSet(Set<String> valuation, Set<String> smallerAlphabet);

    ValuationSet createValuationSetSet(Set<Set<String>> valuation);

    Set<String> getAlphabet();
}
