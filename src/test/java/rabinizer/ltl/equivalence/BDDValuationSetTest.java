package rabinizer.ltl.equivalence;

import rabinizer.collections.valuationset.BDDValuationSetFactory;
import rabinizer.collections.valuationset.ValuationSetFactory;
import rabinizer.ltl.ValuationSetTest;

import java.util.Set;

public class BDDValuationSetTest extends ValuationSetTest {

    @Override
    public ValuationSetFactory setUpFactory(Set<String> domain) {
        return new BDDValuationSetFactory(domain);
    }
}