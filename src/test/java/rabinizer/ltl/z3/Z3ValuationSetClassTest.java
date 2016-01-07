package rabinizer.ltl.z3;

import rabinizer.ltl.ValuationSetFactory;
import rabinizer.ltl.ValuationSetTest;

import java.util.Set;

public class Z3ValuationSetClassTest extends ValuationSetTest {

    @Override
    public ValuationSetFactory<String> setUpFactory(Set<String> domain) {
        return new Z3ValuationSetFactory(domain);
    }
}