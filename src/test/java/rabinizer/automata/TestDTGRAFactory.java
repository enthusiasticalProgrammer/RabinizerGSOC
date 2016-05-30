package rabinizer.automata;

import static org.junit.Assert.*;


import org.junit.Test;

import rabinizer.Util;
import rabinizer.collections.valuationset.ValuationSetFactory;
import rabinizer.exec.FactoryRegistry;
import ltl.Formula;
import ltl.equivalence.EquivalenceClassFactory;

public class TestDTGRAFactory {

    @Test
    public void testAcceptance1() { // It is created to reproduce a bug
        Formula formula = Util.createFormula("G(a | G b)");

        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(formula);
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(formula);

        DTGRA dtgra = DTGRAFactory.constructDTGRA(formula, factory, val, AutomatonClassTest.standard);
        assertTrue(dtgra.acc.stream().allMatch(pair -> pair.infs.stream().allMatch(p -> !p.isEmpty())));
    }

}
