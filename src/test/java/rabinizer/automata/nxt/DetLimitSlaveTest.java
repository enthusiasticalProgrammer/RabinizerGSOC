package rabinizer.automata.nxt;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import rabinizer.exec.Main;
import rabinizer.ltl.*;
import rabinizer.ltl.bdd.BDDEquivalenceClassFactory;
import rabinizer.ltl.bdd.BDDValuationSetFactory;

import static org.junit.Assert.*;

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
        automaton = new DetLimitSlave(formula, false, equivalenceClassFactory, valuationSetFactory);
        automatonImp = new DetLimitSlave(formula, true, equivalenceClassFactory, valuationSetFactory);
    }

    @Test
    public void testGenerateInitialState() throws Exception {
        assertEquals(automaton.generateInitialState(), new DetLimitSlaveState(equivalenceClassFactory.createEquivalenceClass(formula), equivalenceClassFactory.createEquivalenceClass(BooleanConstant.TRUE)));
    }

    @Test
    public void testGenerateSuccState() throws Exception {
        DetLimitSlaveState initialState = automaton.generateInitialState();

        assertEquals(initialState, automaton.generateSuccState(initialState, valuationSetFactory.createValuationSet(ImmutableSet.of("a"))));
        assertNotEquals(initialState, automaton.generateSuccState(initialState, valuationSetFactory.createValuationSet(ImmutableSet.of("b"))));
    }

    @Test
    public void testGenerate() {
        automaton.generate();
        automatonImp.generate();
        assertEquals(6, automaton.size());
        assertEquals(4, automatonImp.size());
    }
}