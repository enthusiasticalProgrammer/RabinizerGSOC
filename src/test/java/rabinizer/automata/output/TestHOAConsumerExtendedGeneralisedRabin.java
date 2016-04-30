package rabinizer.automata.output;

import static org.junit.Assert.*;

import java.util.Collections;

import org.junit.Test;

import jhoafparser.consumer.HOAConsumerException;
import rabinizer.Util;
import rabinizer.automata.Automaton;
import rabinizer.automata.DTGRA;
import rabinizer.automata.DTGRAFactory;
import rabinizer.collections.valuationset.ValuationSetFactory;
import rabinizer.exec.FactoryRegistry;
import rabinizer.exec.FactoryRegistry.Backend;
import rabinizer.ltl.Formula;
import rabinizer.ltl.equivalence.EquivalenceClassFactory;

public class TestHOAConsumerExtendedGeneralisedRabin extends TestHOAConsumerExtended {


    @Test
    public void testTransitionMerging() throws HOAConsumerException {
        Formula f = Util.createFormula("X F (a & c)");
        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(Backend.BDD, f);
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(Backend.BDD, f);
        DTGRA dtgra = DTGRAFactory.constructDTGRA(f, factory, val, Collections.emptySet());

        HOAConsumerGeneralisedRabin ho = new HOAConsumerGeneralisedRabin(null, val);
        ho.addState(dtgra.getInitialState());

        assertEquals(1, ho.getMaximallyMergedEdgesOfEdge(val.createUniverseValuationSet()).size());
    }

    @Override
    protected Automaton getAutomaton() {
        Formula f = Util.createFormula("G(!a | X(X(!a))) & (F b) & (c U d)");
        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(Backend.BDD, f);
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(Backend.BDD, f);
        return DTGRAFactory.constructDTGRA(f, factory, val, Collections.emptySet());
    }
}
