package rabinizer.ltl.bdd;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rabinizer.automata.DTGRARaw;
import rabinizer.exec.Main;
import rabinizer.ltl.*;

import java.util.Set;

import static org.junit.Assert.assertNotEquals;

public class BDDEquivalenceClassTest extends EquivalenceClassTest {

    boolean silent;
    boolean verbose;

    @Before
    public void setup() {
        silent = Main.silent;
        verbose = Main.verbose;
        Main.silent = true;
        Main.verbose = false;
    }

    @After
    public void cleanup() {
        Main.silent = silent;
        Main.verbose = verbose;
    }

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

    /**
     * the test is there, not to assert something, but to see that no exception
     * gets thrown
     */
    @Test
    public void issue9() {
        Formula f = Util.createFormula("true");
        EquivalenceClassFactory factory = new BDDEquivalenceClassFactory(f.getPropositions());
        EquivalenceClass clazz = factory.createEquivalenceClass(f);
        assertNotEquals(null, clazz);
    }

    /**
     * test is there to see that no exception gets thrown
     */
    @Test
    public void issue10() {
        Formula f = Util.createFormula("(G(!(X(G(F(!(G((!(p2)) | (F((p2) U (p1)))))))))))");
        EquivalenceClassFactory factory = new BDDEquivalenceClassFactory(f.getPropositions());
        ValuationSetFactory valuationSetFactory = new BDDValuationSetFactory(f.getAtoms());
        DTGRARaw dtgra = new DTGRARaw(f, true, false, false, false, false, false, factory, valuationSetFactory);
        assertNotEquals(null, dtgra);
    }
}