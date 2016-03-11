package rabinizer.automata;

import java.util.EnumSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import rabinizer.Util;
import rabinizer.exec.FactoryRegistry;
import rabinizer.ltl.equivalence.EquivalenceClassFactory;
import rabinizer.exec.Main;
import rabinizer.ltl.*;
import rabinizer.exec.FactoryRegistry.Backend;
import rabinizer.ltl.simplifier.Simplifier;
import rabinizer.collections.valuationset.ValuationSetFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class AutomatonClassTest {
    private boolean silent;

    private Set<Optimisation> standard = EnumSet.of(Optimisation.COMPUTE_ACC_CONDITION, Optimisation.COMPLETE);

    private Set<Optimisation> standardWithEmpty = EnumSet.of(Optimisation.COMPUTE_ACC_CONDITION, Optimisation.COMPLETE,
            Optimisation.EMPTINESS_CHECK);

    @Before
    public final void setUp() {
        silent = Main.silent;
        Main.silent = true;
    }

    @After
    public final void tearDown() {
        Main.silent = silent;
    }

    @Test
    public void testMasterFoldedNew() {
        Formula f1 = new Literal("p1", false);
        Formula f2 = new Literal("p1", false);
        Formula f3 = Simplifier.simplify(new Disjunction(f1, f2), Simplifier.Strategy.PROPOSITIONAL);
        Formula f4 = new GOperator(f3);

        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(f4.getPropositions());
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(f4.getAtoms());

        Master m = new Master(f4, factory, val, Collections.emptySet(), true);
        assertEquals(f4, m.generateInitialState().getClazz().getRepresentative());
    }

    /**
     * the test ist just there in order to see if there are no exceptions
     */
    @Test
    public void testDTGRARawConstructor() {
        Formula formula = Util.createFormula("(p1) U (X((G(F(G(p2)))) & (F(X(X(G(p2)))))))");

        EquivalenceClassFactory equivalenceClassFactory = FactoryRegistry
                .createEquivalenceClassFactory(formula.getPropositions());
        ValuationSetFactory valuationSetFactory = FactoryRegistry.createValuationSetFactory(formula.getAtoms());

        DTGRA dtgra = DTGRAFactory.constructDTGRA(formula, equivalenceClassFactory,
                valuationSetFactory, standard);
        assertNotNull(dtgra);
    }

    @Test
    public void testSCC1() {
        Formula formula = Util.createFormula("(p1&(p2|p3))");

        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(formula.getPropositions());
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(formula.getAtoms());

        DTGRA dtgra = DTGRAFactory.constructDTGRA(formula, factory, val, standardWithEmpty);
        List<TranSet<Product.ProductState>> SCC = dtgra.SCCs();
        assertEquals(SCC.size(), 3);
    }

    @Test
    public void testSCC2() {
        Formula formula = Util.createFormula("(X a) & (X X a) & (X X X a) & (X X X X a)");

        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(formula.getPropositions());
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(formula.getAtoms());

        DTGRA dtgra = DTGRAFactory.constructDTGRA(formula, factory, val, standardWithEmpty);
        List<TranSet<Product.ProductState>> SCC = dtgra.SCCs();
        assertEquals(SCC.size(), 7);

    }

    @Test
    public void testSCCtopSort1() {
        Formula formula = Util.createFormula("(p1&(p2|p3))");

        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(formula.getPropositions());
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(formula.getAtoms());

        DTGRA dtgra = DTGRAFactory.constructDTGRA(formula, factory, val, standardWithEmpty);
        List<TranSet<Product.ProductState>> SCC = dtgra.SCCs();
        assertTrue(SCC.get(2).asMap().keySet().isEmpty());
    }

    @Test
    public void testSCCtopSort2() {
        Formula formula = Util.createFormula("(X a) & (X X a) & (X X X a) & (X X X X a)");

        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(formula.getPropositions());
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(formula.getAtoms());

        DTGRA dtgra = DTGRAFactory.constructDTGRA(formula, factory, val, standardWithEmpty);
        List<TranSet<Product.ProductState>> SCC = dtgra.SCCs();

        DTGRA dtgra2 = DTGRAFactory.constructDTGRA(formula, factory, val, standardWithEmpty);

        assertTrue(SCC.get(6).asMap().keySet().stream()
                .allMatch(s -> s.primaryState.clazz.equals(dtgra2.initialState.primaryState.clazz)));

        Formula f2 = Util.createFormula("(X a) & (X X a) & (X X X a) & a");
        assertTrue(SCC.get(5).asMap().keySet().stream()
                .allMatch(s -> s.primaryState.clazz
                        .equals(DTGRAFactory.constructDTGRA(f2, factory, val, standard).initialState.primaryState.clazz)));

        Formula f3 = Util.createFormula("(X a) & (X X a)  & a");
        assertTrue(SCC.get(4).asMap().keySet().stream()
                .allMatch(s -> s.primaryState.clazz
                        .equals(DTGRAFactory.constructDTGRA(f3, factory, val, standard).initialState.primaryState.clazz)));

        Formula f4 = Util.createFormula("(X a)  & a");
        assertTrue(SCC.get(3).asMap().keySet().stream()
                .allMatch(s -> s.primaryState.clazz
                        .equals(DTGRAFactory.constructDTGRA(f4, factory, val, standard).initialState.primaryState.clazz)));

        Formula f5 = Util.createFormula("a");
        assertTrue(SCC.get(2).asMap().keySet().stream()
                .allMatch(s -> s.primaryState.clazz
                        .equals(DTGRAFactory.constructDTGRA(f5, factory, val, standard).initialState.primaryState.clazz)));
    }

    @Test
    public void testIsSink1() {
        Formula formula = Util.createFormula("(X a) & (X X a) & (X X X a) & (X X X X a)");

        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(formula.getPropositions());
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(formula.getAtoms());

        DTGRA dtgra = DTGRAFactory.constructDTGRA(formula, factory, val, standard);
        List<TranSet<Product.ProductState>> SCC = dtgra.SCCs();

        assertTrue(dtgra.isSink(SCC.get(0).asMap().keySet()));
    }

    @Test
    public void checkIfStatesGetLostInTheDTGRATranslation() {
        Formula f = Util.createFormula("true");
        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(f.getPropositions());
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(f.getAtoms());
        DTGRA dtgra = DTGRAFactory.constructDTGRA(f, factory, val, standard);
        assertEquals(dtgra.states.size(), 1);

        dtgra = DTGRAFactory.constructDTGRA(f, factory, val, standard);
        assertEquals(dtgra.states.size(), 1);
    }

    @Test
    public void checkIfStatesGetLostInTheDTGRATranslation2() {
        Formula f = Util.createFormula("F a & G b");
        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(f.getPropositions());
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(f.getAtoms());
        DTGRA dtgra = DTGRAFactory.constructDTGRA(f, factory, val, standard);
        assertFalse(dtgra.states.isEmpty());

        dtgra = DTGRAFactory.constructDTGRA(f, factory, val, standard);
        assertEquals(dtgra.states.size(), 3);
    }

    @Test
    public void checkNullNotInTransition() {
        Formula f=Util.createFormula("p0");
        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(f.getPropositions());
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(f.getAtoms());
        DTGRA dtgra = DTGRAFactory.constructDTGRA(f, factory, val, standard);
        assertFalse(dtgra.transitions.values().contains(null));
    }

    @Test
    public void checkMissingState() {
        Formula f=Util.createFormula("p0");
        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(f.getPropositions());
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(f.getAtoms());
        DTGRA dtgra = DTGRAFactory.constructDTGRA(f, factory, val, standard);
        assertTrue(dtgra.states.size() == 3);
    }

    @Test
    public void checkEmptinessCheck() {
        Formula f = Util.createFormula("G(p0)");
        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(f.getPropositions());
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(f.getAtoms());
        DTGRA dtgra = DTGRAFactory.constructDTGRA(f, factory, val, standard);
        assertFalse(EmptinessCheck.checkEmptiness(dtgra, dtgra.acc));
    }

    @Test
    public void testRabinSlaveSuccessors() {
        Formula f = Util.createFormula("G(a)");
        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(f.getPropositions());
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(f.getAtoms());
        MojmirSlave mSlave = new MojmirSlave((GOperator) f, factory, val, EnumSet.of(Optimisation.EAGER));
        mSlave.generate();
        mSlave.removeSinks();

        RabinSlave rSlave = new RabinSlave(mSlave, val);
        assertFalse(rSlave.getInitialState().getSuccessors().keySet().isEmpty());
    }

    @Test
    public void testNotExceptionOccurring() {
        Formula f = Util.createFormula("G(a)");

        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(Backend.Z3, f.getPropositions());
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(Backend.Z3, f.getAtoms());
        DTGRA dtgra = DTGRAFactory.constructDTGRA(f, factory, val, EnumSet.of(Optimisation.COMPUTE_ACC_CONDITION));
        assertNotNull(dtgra);

        factory = FactoryRegistry.createEquivalenceClassFactory(Backend.BDD, f.getPropositions());
        val = FactoryRegistry.createValuationSetFactory(Backend.BDD, f.getAtoms());
        dtgra = DTGRAFactory.constructDTGRA(f, factory, val, EnumSet.of(Optimisation.COMPUTE_ACC_CONDITION));
        assertNotNull(dtgra);

    }

    @Test
    public void testEmptinessCheck() {
        Formula f = Util.createFormula("G(!a | X(X(!a)))");

        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(Backend.BDD, f.getPropositions());
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(Backend.BDD, f.getAtoms());
        DTGRA dtgra = DTGRAFactory.constructDTGRA(f, factory, val, EnumSet.of(Optimisation.COMPUTE_ACC_CONDITION));
        assertFalse(EmptinessCheck.checkEmptiness(dtgra, dtgra.acc));
    }

    @Test
    public void testSCC3() {
        Formula f = Util.createFormula("G(!a | X(X(!a)))");

        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(Backend.BDD, f.getPropositions());
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(Backend.BDD, f.getAtoms());
        DTGRA dtgra = DTGRAFactory.constructDTGRA(f, factory, val, EnumSet.of(Optimisation.COMPUTE_ACC_CONDITION));
        assertEquals(dtgra.SCCs().size(), 1);
    }
}
