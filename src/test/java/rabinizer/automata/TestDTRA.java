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

        DTGRAFactory automatonFactory = new DTGRAFactory(formula, equivalenceClassFactory, valuationSetFactory, AutomatonClassTest.standard);
        Product dtgra = automatonFactory.constructAutomaton();
        DTRA dtra = new DTRA(dtgra);
        assertTrue(dtra.getStates().size() < 10);
    }

}
