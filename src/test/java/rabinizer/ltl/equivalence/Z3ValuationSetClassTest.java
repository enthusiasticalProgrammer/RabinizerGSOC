package rabinizer.ltl.equivalence;

import rabinizer.collections.valuationset.ValuationSetFactory;
import rabinizer.ltl.ValuationSetTest;
import rabinizer.collections.valuationset.Z3ValuationSetFactory;

import java.util.Set;

public class Z3ValuationSetClassTest extends ValuationSetTest {

    @Override
    public ValuationSetFactory setUpFactory(Set<String> domain) {
        return new Z3ValuationSetFactory(domain);
    }
}