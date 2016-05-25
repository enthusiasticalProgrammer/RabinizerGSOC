package rabinizer.automata.output;

import static org.junit.Assert.*;

import java.util.Collections;

import org.junit.Test;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

import jhoafparser.consumer.HOAConsumerException;
import rabinizer.Util;
import rabinizer.automata.Automaton;
import rabinizer.automata.DTGRA;
import rabinizer.automata.DTGRAFactory;
import rabinizer.collections.valuationset.ValuationSetFactory;
import rabinizer.exec.FactoryRegistry;
import rabinizer.exec.FactoryRegistry.Backend;
import ltl.Formula;
import ltl.equivalence.EquivalenceClassFactory;

public class TestHOAConsumerExtendedGeneralisedRabin extends TestHOAConsumerExtended {

    @Test
    public void testTransitionMerging() throws HOAConsumerException {
        Formula f = Util.createFormula("X F (a & c)");
        BiMap<String, Integer> aliases = ImmutableBiMap.of("a", 0, "c", 1);
        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(Backend.BDD, f);
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(Backend.BDD, f, aliases);
        DTGRA dtgra = DTGRAFactory.constructDTGRA(f, factory, val, Collections.emptySet());

        HOAConsumerGeneralisedRabin ho = (HOAConsumerGeneralisedRabin) dtgra.getConsumer(getTestConsumer());
        assertEquals(1, ho.getMaximallyMergedEdgesOfEdge(val.createUniverseValuationSet()).size());
    }

    @Override
    protected Automaton getAutomaton() {
        Formula f = Util.createFormula("G(!a | X(X(!a))) & (F b) & (c U d)");
        BiMap<String, Integer> aliases = ImmutableBiMap.of("a", 0, "b", 1, "c", 2, "d", 3);
        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(Backend.BDD, f);
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(Backend.BDD, f, aliases);
        return DTGRAFactory.constructDTGRA(f, factory, val, Collections.emptySet());
    }
}
