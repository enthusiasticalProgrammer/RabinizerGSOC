package rabinizer.automata;

import static org.junit.Assert.*;


import org.junit.Test;

import rabinizer.Util;
import omega_automaton.collections.valuationset.*;
import ltl.Formula;
import ltl.equivalence.EquivalenceClassFactory;

public class TestDTGRAFactory {

    @Test
    public void testAcceptance1() { // It is created to reproduce a bug
        Formula formula = Util.createFormula("G(a | G b)");

        EquivalenceClassFactory factory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(formula);
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(formula);

        DTGRA dtgra = DTGRAFactory.constructDTGRA(formula, factory, val, AutomatonClassTest.standard);
        assertTrue(dtgra.acceptance.acceptanceCondition.stream().allMatch(pair -> pair.right.stream().allMatch(p -> !p.isEmpty())));
    }

}
