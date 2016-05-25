package rabinizer.automata.output;

import java.util.Collections;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

import rabinizer.Util;
import rabinizer.automata.Automaton;
import rabinizer.automata.DTGRAFactory;
import rabinizer.collections.valuationset.ValuationSetFactory;
import rabinizer.exec.FactoryRegistry;
import rabinizer.exec.FactoryRegistry.Backend;
import ltl.Formula;
import ltl.equivalence.EquivalenceClassFactory;

public class TestHOAConsumerExtendedRabin extends TestHOAConsumerExtended {

    @Override
    protected Automaton getAutomaton() {
        Formula f = Util.createFormula("(F b) & (c U d)");
        BiMap<String, Integer> aliases = ImmutableBiMap.of("b", 0, "c", 1, "d", 2);
        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(Backend.BDD, f);
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(Backend.BDD, f, aliases);
        return DTGRAFactory.constructDTGRA(f, factory, val, Collections.emptySet());
    }
}
