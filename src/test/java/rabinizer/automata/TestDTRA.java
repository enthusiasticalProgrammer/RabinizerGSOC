package rabinizer.automata;

import static org.junit.Assert.*;

import org.junit.Test;

import rabinizer.Util;
import rabinizer.collections.valuationset.ValuationSetFactory;
import rabinizer.exec.FactoryRegistry;
import rabinizer.ltl.Formula;
import rabinizer.ltl.equivalence.EquivalenceClassFactory;

public class TestDTRA {

    @Test
    public void testDTGRARawConstructor() {
        Formula formula = Util.createFormula("true");

        EquivalenceClassFactory equivalenceClassFactory = FactoryRegistry.createEquivalenceClassFactory(formula);
        ValuationSetFactory valuationSetFactory = FactoryRegistry.createValuationSetFactory(formula);

        DTGRA dtgra = DTGRAFactory.constructDTGRA(formula, equivalenceClassFactory, valuationSetFactory, AutomatonClassTest.standard);
        DTRA dtra = new DTRA(dtgra);
        assertTrue(dtra.getStates().size() < 10);
    }

}
