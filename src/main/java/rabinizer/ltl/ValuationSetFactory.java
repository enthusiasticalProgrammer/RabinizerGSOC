package rabinizer.ltl;

import java.util.Set;

public interface ValuationSetFactory<E> {

    public Set<E> getAlphabet();

    public ValuationSet createEmptyValuationSet();

    public ValuationSet createUniverseValuationSet();

    public ValuationSet createValuationSet(Set<E> valuation);

    public ValuationSet createValuationSet2(Set<Set<E>> valuation);

    public ValuationSet createValuationSet(ValuationSet sets);

    public Set<ValuationSet> createAllValuationSets();
}
