package rabinizer.ltl.z3;

import java.util.Set;

import rabinizer.ltl.EquivalenceClassFactory;
import rabinizer.ltl.EquivalenceClassTest;
import rabinizer.ltl.Formula;

public class Z3EquivalenceClassTest extends EquivalenceClassTest {

    @Override
    public EquivalenceClassFactory setUpFactory(Set<Formula> domain) {
        return new Z3EquivalenceClassFactory(domain);
    }

}
