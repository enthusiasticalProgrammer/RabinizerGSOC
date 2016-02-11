package rabinizer.ltl.equivalence;

import rabinizer.ltl.Formula;

import java.util.Set;

public class Z3EquivalenceClassTest extends EquivalenceClassTest {

    @Override
    public EquivalenceClassFactory setUpFactory(Set<Formula> domain) {
        return new Z3EquivalenceClassFactory(domain);
    }

}
