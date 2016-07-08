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
        ValuationSetFactory val = new BDDValuationSetFactory(2);

        DTGRAFactory automatonFactory = new DTGRAFactory(formula, factory, val, AutomatonClassTest.standard);
        ProductRabinizer dtgra = automatonFactory.constructAutomaton();
        assertTrue(dtgra.getAcceptance().acceptanceCondition.stream().allMatch(pair -> pair.right.stream().allMatch(p -> !p.isEmpty())));
    }

}
