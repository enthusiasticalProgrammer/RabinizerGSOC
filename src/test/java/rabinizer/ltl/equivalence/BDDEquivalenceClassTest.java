package rabinizer.ltl.equivalence;

import org.junit.Test;
import rabinizer.Util;
import rabinizer.ltl.equivalence.BDDEquivalenceClassFactory;
import rabinizer.ltl.equivalence.EquivalenceClass;
import rabinizer.ltl.equivalence.EquivalenceClassFactory;
import rabinizer.ltl.*;
import rabinizer.ltl.equivalence.EquivalenceClassTest;

import java.util.Set;

import static org.junit.Assert.assertNotEquals;

public class BDDEquivalenceClassTest extends EquivalenceClassTest {

    @Override
    public EquivalenceClassFactory setUpFactory(Set<Formula> domain) {
        return new BDDEquivalenceClassFactory(domain);
    }

    @Test
    public void issue6() throws Exception {
        Formula f = Util.createFormula("(p1) U (p2 & G(p2 & !p1))");
        EquivalenceClassFactory factory = new BDDEquivalenceClassFactory(f.getPropositions());
        EquivalenceClass clazz = factory.createEquivalenceClass(f);
        assertNotEquals(null, clazz);
    }
}