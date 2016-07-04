package rabinizer.automata;

import java.util.EnumSet;

import org.junit.Before;
import org.junit.Test;

import rabinizer.Util;
import rabinizer.automata.DTGRAFactory;
import rabinizer.automata.EmptinessCheck;
import rabinizer.automata.Optimisation;
import rabinizer.automata.Product;
import rabinizer.automata.Product.ProductState;
import ltl.equivalence.EquivalenceClassFactory;
import ltl.equivalence.FactoryRegistry.Backend;
import omega_automaton.Edge;
import omega_automaton.algorithms.SCCAnalyser;
import omega_automaton.collections.valuationset.*;
import rabinizer.exec.Main;
import ltl.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import static org.junit.Assert.*;

public class AutomatonClassTest {

    static final Set<Optimisation> standard = EnumSet.of(Optimisation.COMPUTE_ACC_CONDITION);
    static final Set<Optimisation> standardWithEmpty = EnumSet.of(Optimisation.COMPUTE_ACC_CONDITION, Optimisation.EMPTINESS_CHECK);

    @Before
    public final void setUp() {
        Main.silent = true;
    }

    @Test
    public void testMasterFoldedNew() {
        Formula f1 = new Literal(0, false);
        Formula f2 = new Literal(0, false);
        Formula f3 = new Disjunction(f1, f2);
        Formula f4 = new GOperator(f3);

        EquivalenceClassFactory factory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(f4);
        ValuationSetFactory val = omega_automaton.collections.valuationset.FactoryRegistry.createValuationSetFactory(f4);

        Master m = new Master(f4, factory, val, Collections.emptySet());
        assertEquals(f4, m.generateInitialState().getClazz().getRepresentative());
    }

    /**
     * the test ist just there in order to see if there are no exceptions
     */
    @Test
    public void testDTGRARawConstructor() {
        Formula formula = Util.createFormula("(p1) U (X((G(F(G(p2)))) & (F(X(X(G(p2)))))))");

        EquivalenceClassFactory equivalenceClassFactory = ltl.equivalence.FactoryRegistry
                .createEquivalenceClassFactory(formula);
        ValuationSetFactory valuationSetFactory = FactoryRegistry.createValuationSetFactory(formula);

        DTGRAFactory automatonFactory = new DTGRAFactory(formula, equivalenceClassFactory, valuationSetFactory, standard);
        Product dtgra = automatonFactory.constructAutomaton();
        assertNotNull(dtgra);
    }

    @Test
    public void testSCC1() {
        Formula formula = Util.createFormula("(p1&(p2|p3))");

        EquivalenceClassFactory factory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(formula);
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(formula);

        DTGRAFactory automatonFactory = new DTGRAFactory(formula, factory, val, standardWithEmpty);
        Product dtgra = automatonFactory.constructAutomaton();
        List<Set<ProductState<?>>> SCC = SCCAnalyser.SCCsStates(dtgra);
        assertEquals(2, SCC.size());
    }

    @Test
    public void testSCC2() {
        Formula formula = Util.createFormula("(X a) & (X X a) & (X X X a) & (X X X X a)");

        EquivalenceClassFactory factory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(formula);
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(formula);

        DTGRAFactory automatonFactory = new DTGRAFactory(formula, factory, val, standardWithEmpty);
        Product dtgra = automatonFactory.constructAutomaton();
        List<Set<ProductState<?>>> SCC = SCCAnalyser.SCCsStates(dtgra);
        assertEquals(6, SCC.size());

    }

    @Test
    public void testSCCtopSort1() {
        Formula formula = Util.createFormula("(p1&(p2|p3))");

        EquivalenceClassFactory factory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(formula);
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(formula);

        DTGRAFactory automatonFactory = new DTGRAFactory(formula, factory, val, standardWithEmpty);
        Product dtgra = automatonFactory.constructAutomaton();
        List<Set<ProductState<?>>> SCC = SCCAnalyser.SCCsStates(dtgra);
        assertFalse(dtgra.isBSCC(SCC.get(1)));
    }

    @Test
    public void testSCCtopSort2() {
        Formula formula = Util.createFormula("(X a) & (X X a) & (X X X a) & (X X X X a)");

        EquivalenceClassFactory factory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(formula);
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(formula);

        DTGRAFactory automatonFactory = new DTGRAFactory(formula, factory, val, standardWithEmpty);
        Product dtgra = automatonFactory.constructAutomaton();
        List<Set<ProductState<?>>> SCC = SCCAnalyser.SCCsStates(dtgra);

        Formula f2 = Util.createFormula("(X a) & (X X a) & (X X X a) & a");

        DTGRAFactory testAutomatonFactory = new DTGRAFactory(formula, factory, val, standard);
        Product dtgraTest = testAutomatonFactory.constructAutomaton();
        assertTrue(SCC.get(5).stream().allMatch(s -> s.primaryState.getClazz().equals(dtgraTest.getInitialState().primaryState.getClazz())));

        testAutomatonFactory = new DTGRAFactory(f2, factory, val, standard);
        Product dtgraTest2 = testAutomatonFactory.constructAutomaton();
        Formula f3 = Util.createFormula("(X a) & (X X a)  & a");
        assertTrue(SCC.get(4).stream().allMatch(s -> s.primaryState.clazz.equals(dtgraTest2.getInitialState().primaryState.clazz)));

        testAutomatonFactory = new DTGRAFactory(f3, factory, val, standard);
        Product dtgraTest3 = testAutomatonFactory.constructAutomaton();
        Formula f4 = Util.createFormula("(X a)  & a");
        assertTrue(SCC.get(3).stream().allMatch(s -> s.primaryState.clazz.equals(dtgraTest3.getInitialState().primaryState.clazz)));

        testAutomatonFactory = new DTGRAFactory(f4, factory, val, standard);
        Product dtgraTest4 = testAutomatonFactory.constructAutomaton();
        assertTrue(SCC.get(2).stream().allMatch(s -> s.primaryState.clazz.equals(dtgraTest4.getInitialState().primaryState.clazz)));
    }

    @Test
    public void testIsSink1() {
        Formula formula = Util.createFormula("(X a) & (X X a) & (X X X a) & (X X X X a)");

        EquivalenceClassFactory factory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(formula);
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(formula);

        DTGRAFactory automatonFactory = new DTGRAFactory(formula, factory, val, standard);
        Product dtgra = automatonFactory.constructAutomaton();
        List<Set<ProductState<?>>> SCC = SCCAnalyser.SCCsStates(dtgra);

        assertTrue(dtgra.isBSCC(SCC.get(0)));
    }

    @Test
    public void checkIfStatesGetLostInTheDTGRATranslation() {
        Formula f = Util.createFormula("true");
        EquivalenceClassFactory factory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(f);
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(f);

        DTGRAFactory automatonFactory = new DTGRAFactory(f, factory, val, standard);
        Product dtgra = automatonFactory.constructAutomaton();
        assertEquals(1, dtgra.size());
    }

    @Test
    public void checkIfStatesGetLostInTheDTGRATranslation2() {
        Formula f = Util.createFormula("F a & G b");
        EquivalenceClassFactory factory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(f);
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(f);
        DTGRAFactory automatonFactory = new DTGRAFactory(f, factory, val, standard);
        Product dtgra = automatonFactory.constructAutomaton();
        assertEquals(2, dtgra.size());
    }

    @Test
    public void checkNullNotInTransition() {
        Formula f=Util.createFormula("p0");
        EquivalenceClassFactory factory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(f);
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(f);
        DTGRAFactory automatonFactory = new DTGRAFactory(f, factory, val, standard);
        Product dtgra = automatonFactory.constructAutomaton();
        assertFalse(dtgra.getSuccessors(dtgra.getInitialState()).values().contains(null));
    }

    @Test
    public void checkMissingState() {
        Formula f=Util.createFormula("p0");
        EquivalenceClassFactory factory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(f);
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(f);
        DTGRAFactory automatonFactory = new DTGRAFactory(f, factory, val, standard);
        Product dtgra = automatonFactory.constructAutomaton();
        assertEquals(2, dtgra.size());
    }

    @Test
    public void checkEmptinessCheck() {
        Formula f = Util.createFormula("G(p0)");
        EquivalenceClassFactory factory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(f);
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(f);
        DTGRAFactory automatonFactory = new DTGRAFactory(f, factory, val, standard);
        Product dtgra = automatonFactory.constructAutomaton();
        assertFalse(EmptinessCheck.checkEmptinessAndMinimiseSCCBasedProduct(dtgra));
    }

    @Test
    public void testRabinSlaveSuccessors() {
        Formula f = Util.createFormula("G(a)");
        EquivalenceClassFactory factory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(f);
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(f);
        MojmirSlave mSlave = new MojmirSlave((ModalOperator) f, factory, val, EnumSet.of(Optimisation.EAGER));
        mSlave.generate();

        RabinSlave rSlave = new RabinSlave(mSlave, val);
        assertFalse(rSlave.getInitialState().getSuccessors().keySet().isEmpty());
    }

    @Test
    public void testNotExceptionOccurring() {
        Formula f = Util.createFormula("G(a)");

        EquivalenceClassFactory factory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(f);
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(f);
        DTGRAFactory automatonFactory = new DTGRAFactory(f, factory, val, standard);
        Product dtgra = automatonFactory.constructAutomaton();
        assertNotNull(dtgra);
    }

    @Test
    public void testEmptinessCheck() {
        Formula f = Util.createFormula("G(!a | X(X(!a)))");

        EquivalenceClassFactory factory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(f);
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(f);
        DTGRAFactory automatonFactory = new DTGRAFactory(f, factory, val, standard);
        Product dtgra = automatonFactory.constructAutomaton();
        assertFalse(EmptinessCheck.checkEmptinessAndMinimiseSCCBasedProduct(dtgra));
    }

    @Test
    public void testSCC3() {
        Formula f = Util.createFormula("G(!a | X(X(!a)))");

        EquivalenceClassFactory factory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(Backend.BDD, f);
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(f);
        DTGRAFactory automatonFactory = new DTGRAFactory(f, factory, val, standard);
        Product dtgra = automatonFactory.constructAutomaton();
        assertEquals(1, SCCAnalyser.SCCsStates(dtgra).size());
    }

    @Test
    public void testEmptinessCheck2() {
        Formula f = Util.createFormula("(G((X(!(X(p2)))) U (p2)))");

        EquivalenceClassFactory factory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(Backend.BDD, f);
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(f);
        DTGRAFactory automatonFactory = new DTGRAFactory(f, factory, val, standard);
        Product dtgra = automatonFactory.constructAutomaton();
        EmptinessCheck.checkEmptinessAndMinimiseSCCBasedProduct(dtgra);
        assertEquals(3, dtgra.getStates().size());
    }

    @Test
    public void testEmptinessCheck3() {
        Formula f = Util.createFormula("a | X X(G b & F(G !b))");

        EquivalenceClassFactory factory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(Backend.BDD, f);
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(f);
        DTGRAFactory automatonFactory = new DTGRAFactory(f, factory, val, standard);
        Product dtgra = automatonFactory.constructAutomaton();
        EmptinessCheck.checkEmptinessAndMinimiseSCCBasedProduct(dtgra);
        assertEquals(2, dtgra.getStates().size());
    }

    @Test
    public void testEmptinessCheck4() {
        Formula f = Util.createFormula("X (G a & F (b U !a))");

        EquivalenceClassFactory factory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(Backend.BDD, f);
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(f);
        DTGRAFactory automatonFactory = new DTGRAFactory(f, factory, val, standard);
        Product dtgra = automatonFactory.constructAutomaton();
        assertTrue(EmptinessCheck.checkEmptinessAndMinimiseSCCBasedProduct(dtgra));
    }

    @Test
    public void testDTGRAValuationSetFactoryNotNull() {
        Formula f = Util.createFormula("X (F a)");

        EquivalenceClassFactory factory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(Backend.BDD, f);
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(f);
        DTGRAFactory automatonFactory = new DTGRAFactory(f, factory, val, standard);
        Product dtgra = automatonFactory.constructAutomaton();
        assertNotNull(dtgra.getFactory());
    }

    @Test
    public void testIfTrueAcceptsAnything() {
        Formula f = Util.createFormula("true");

        EquivalenceClassFactory factory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(Backend.BDD, f);
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(f);
        DTGRAFactory automatonFactory = new DTGRAFactory(f, factory, val, standardWithEmpty);
        Product dtgra = automatonFactory.constructAutomaton();
        assertNotEquals(0, dtgra.getAcceptance().getAcceptanceSets());
    }

    @Test
    public void testIsDeterministic() {
        Formula formula = Util.createFormula("G  (a & X a)");

        EquivalenceClassFactory equivalenceClassFactory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(formula);
        ValuationSetFactory valuationSetFactory = FactoryRegistry.createValuationSetFactory(formula);

        DTGRAFactory automatonFactory = new DTGRAFactory(formula, equivalenceClassFactory, valuationSetFactory, standard);
        ProductRabinizer dtgra = automatonFactory.constructAutomaton();

        for (ProductState<?> state : dtgra.getStates()) {
            ValuationSet valu = valuationSetFactory.createEmptyValuationSet();
            for (Entry<Edge<ProductState<?>>, ValuationSet> edge : dtgra.getSuccessors(state).entrySet()) {
                assertFalse(valu.intersects(edge.getValue()));
                valu.addAll(edge.getValue());
            }
        }
    }

    @Test
    public void testRabinSlaveIsDeterministic() {
        Formula formula = Util.createFormula("G  (a & X a) | b");

        EquivalenceClassFactory equivalenceClassFactory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(formula);
        ValuationSetFactory valuationSetFactory = FactoryRegistry.createValuationSetFactory(formula);

        DTGRAFactory automatonFactory = new DTGRAFactory(formula, equivalenceClassFactory, valuationSetFactory, standard);
        ProductRabinizer dtgra = automatonFactory.constructAutomaton();

        for (ProductState<?> state : dtgra.getStates()) {
            ValuationSet valu = valuationSetFactory.createEmptyValuationSet();
            for (Entry<Edge<ProductState<?>>, ValuationSet> edge : dtgra.getSuccessors(state).entrySet()) {
                assertFalse(valu.intersects(edge.getValue()));
                valu.addAll(edge.getValue());
            }
        }
    }
}
