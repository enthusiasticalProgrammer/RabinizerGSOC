package rabinizer.ltl.bdd;

import rabinizer.ltl.EquivalenceClassFactory;
import rabinizer.ltl.EquivalenceClassTest;
import rabinizer.ltl.Formula;

import java.util.Set;

public class BDDEquivalenceClassTest extends EquivalenceClassTest {

    @Override
    public EquivalenceClassFactory setUpFactory(Set<Formula> domain) {
        return new BDDEquivalenceClassFactory(domain);
    }
}