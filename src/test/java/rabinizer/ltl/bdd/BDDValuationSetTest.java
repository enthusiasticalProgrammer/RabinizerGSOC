package rabinizer.ltl.bdd;

import rabinizer.ltl.ValuationSetFactory;
import rabinizer.ltl.ValuationSetTest;

import java.util.Set;

public class BDDValuationSetTest extends ValuationSetTest {

    @Override
    public ValuationSetFactory<String> setUpFactory(Set<String> domain) {
        return new BDDValuationSetFactory(domain);
    }
}