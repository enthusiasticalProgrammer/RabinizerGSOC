package rabinizer.automata.nxt;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import rabinizer.automata.IState;
import rabinizer.automata.Optimisation;
import rabinizer.ltl.equivalence.EquivalenceClassFactory;
import rabinizer.ltl.*;
import rabinizer.ltl.equivalence.BDDEquivalenceClassFactory;
import rabinizer.collections.valuationset.BDDValuationSetFactory;
import rabinizer.collections.valuationset.ValuationSetFactory;

import java.util.BitSet;
import java.util.Collections;
import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class DetLimitSlaveTest {

    private Formula formula;
    private ValuationSetFactory valuationSetFactory;
    private EquivalenceClassFactory equivalenceClassFactory;
    private DetLimitSlave automaton;
    private DetLimitSlave automatonImp;

    @Before
    public void setUp() {
        formula = new Disjunction(new FOperator(new Literal(0, false)), new XOperator(new Literal(1, false)));
        valuationSetFactory = new BDDValuationSetFactory(formula);
        equivalenceClassFactory = new BDDEquivalenceClassFactory(formula);
        automaton = new DetLimitSlave(equivalenceClassFactory.createEquivalenceClass(formula), equivalenceClassFactory, valuationSetFactory, Collections.emptySet());
        automatonImp = new DetLimitSlave(equivalenceClassFactory.createEquivalenceClass(formula), equivalenceClassFactory, valuationSetFactory,
                EnumSet.allOf(Optimisation.class));
    }

    @Test
    public void testGenerateSuccState() throws Exception {
        IState initialState = automaton.generateInitialState();

        //assertEquals(initialState, initialState.getSuccessor(new BitSet()));
        assertNotEquals(initialState, initialState.getSuccessor(new BitSet()));
    }

    @Test
    public void testGenerate() {
        automaton.generate();
        automatonImp.generate();
        assertEquals(6, automaton.size());
        assertEquals(4, automatonImp.size());
    }
}