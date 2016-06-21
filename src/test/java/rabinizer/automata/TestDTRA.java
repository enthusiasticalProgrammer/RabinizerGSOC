package rabinizer.automata;

import static org.junit.Assert.*;

import org.junit.Test;

import rabinizer.Util;
import omega_automaton.collections.valuationset.*;
import ltl.Formula;
import ltl.equivalence.EquivalenceClassFactory;

public class TestDTRA {

    @Test
    public void testDTGRARawConstructor() {
        Formula formula = Util.createFormula("true");

        EquivalenceClassFactory equivalenceClassFactory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(formula);
        ValuationSetFactory valuationSetFactory = FactoryRegistry.createValuationSetFactory(formula);

        DTGRA dtgra = DTGRAFactory.constructDTGRA(formula, equivalenceClassFactory, valuationSetFactory, AutomatonClassTest.standard);
        DTRA dtra = new DTRA(dtgra);
        assertTrue(dtra.getStates().size() < 10);
    }

}
