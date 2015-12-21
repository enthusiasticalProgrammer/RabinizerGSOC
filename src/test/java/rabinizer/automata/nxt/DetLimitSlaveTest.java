package rabinizer.automata.nxt;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import rabinizer.automata.IState;
import rabinizer.automata.Optimisation;
import rabinizer.ltl.*;
import rabinizer.ltl.bdd.BDDEquivalenceClassFactory;
import rabinizer.ltl.bdd.BDDValuationSetFactory;

import java.util.Collections;
import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class DetLimitSlaveTest {

    private Formula formula;
    private ValuationSetFactory<String> valuationSetFactory;
    private EquivalenceClassFactory equivalenceClassFactory;
    private DetLimitSlave automaton;
    private DetLimitSlave automatonImp;

    @Before
    public void setUp() {
        formula = new Disjunction(new FOperator(new Literal("a", false)), new XOperator(new Literal("b", false)));
        valuationSetFactory = new BDDValuationSetFactory(formula.getAtoms());
        equivalenceClassFactory = new BDDEquivalenceClassFactory(formula.getPropositions());
        automaton = new DetLimitSlave(formula, equivalenceClassFactory, valuationSetFactory, Collections.emptySet());
        automatonImp = new DetLimitSlave(formula, equivalenceClassFactory, valuationSetFactory,
                EnumSet.allOf(Optimisation.class));
    }

    @Test
    public void testGenerateSuccState() throws Exception {
        IState initialState = automaton.generateInitialState();

        assertEquals(initialState, initialState.getSuccessor(ImmutableSet.of("a")));
        assertNotEquals(initialState, initialState.getSuccessor(ImmutableSet.of("b")));
    }

    @Test
    public void testGenerate() {
        automaton.generate();
        automatonImp.generate();
        assertEquals(6, automaton.size());
        assertEquals(4, automatonImp.size());
    }
}