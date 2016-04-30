package rabinizer.automata.output;

import java.util.Collections;

import rabinizer.Util;
import rabinizer.automata.Automaton;
import rabinizer.automata.DTGRA;
import rabinizer.automata.DTGRAFactory;
import rabinizer.automata.DTRA;
import rabinizer.collections.valuationset.ValuationSetFactory;
import rabinizer.exec.FactoryRegistry;
import rabinizer.exec.FactoryRegistry.Backend;
import rabinizer.ltl.Formula;
import rabinizer.ltl.equivalence.EquivalenceClassFactory;

public class TestHOAConsumerExtendedRabin extends TestHOAConsumerExtended {

    @Override
    protected Automaton getAutomaton() {
        Formula f = Util.createFormula("(F b) & (c U d)");
        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(Backend.BDD, f);
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(Backend.BDD, f);
        return DTGRAFactory.constructDTGRA(f, factory, val, Collections.emptySet());
    }
}
