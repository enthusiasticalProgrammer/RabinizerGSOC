package rabinizer.automata;

import java.util.EnumSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import rabinizer.Util;
import rabinizer.ltl.equivalence.EquivalenceClassFactory;
import rabinizer.exec.Main;
import rabinizer.ltl.*;
import rabinizer.ltl.FactoryRegistry.Backend;
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

        DTGRARaw dtgra = new DTGRARaw(formula, equivalenceClassFactory,
                valuationSetFactory, standard);
        assertNotNull(dtgra);
    }

    @Test
    public void testSCC1() {
        Formula formula = Util.createFormula("(p1&(p2|p3))");

        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(formula.getPropositions());
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(formula.getAtoms());

        DTGRARaw dtgra = new DTGRARaw(formula, factory, val, standardWithEmpty);
        List<Set<Product.ProductState>> SCC = dtgra.automaton.SCCs();
        assertEquals(SCC.size(), 3);

    }

    @Test
    public void testSCC2() {
        Formula formula = Util.createFormula("(X a) & (X X a) & (X X X a) & (X X X X a)");

        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(formula.getPropositions());
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(formula.getAtoms());

        DTGRARaw dtgra = new DTGRARaw(formula, factory, val, standardWithEmpty);
        List<Set<Product.ProductState>> SCC = dtgra.automaton.SCCs();
        assertEquals(SCC.size(), 7);

    }

    @Test
    public void testSCCtopSort1() {
        Formula formula = Util.createFormula("(p1&(p2|p3))");

        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(formula.getPropositions());
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(formula.getAtoms());

        DTGRARaw dtgra = new DTGRARaw(formula, factory, val, standardWithEmpty);
        List<Set<Product.ProductState>> SCC = dtgra.automaton.SCCs();
        assertTrue(SCC.get(2).contains(dtgra.automaton.initialState));
    }

    @Test
    public void testSCCtopSort2() {
        Formula formula = Util.createFormula("(X a) & (X X a) & (X X X a) & (X X X X a)");

        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(formula.getPropositions());
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(formula.getAtoms());

        DTGRARaw dtgra = new DTGRARaw(formula, factory, val, standardWithEmpty);
        List<Set<Product.ProductState>> SCC = dtgra.automaton.SCCs();

        DTGRARaw dtgra2 = new DTGRARaw(formula, factory, val, standardWithEmpty);

        assertTrue(SCC.get(6).stream()
                .allMatch(s -> s.primaryState.clazz.equals(dtgra2.automaton.initialState.primaryState.clazz)));

        Formula f2 = Util.createFormula("(X a) & (X X a) & (X X X a) & a");
        assertTrue(SCC.get(5).stream().allMatch(s -> s.primaryState.clazz
                .equals(new DTGRARaw(f2, factory, val, standard).automaton.initialState.primaryState.clazz)));

        Formula f3 = Util.createFormula("(X a) & (X X a)  & a");
        assertTrue(SCC.get(4).stream().allMatch(s -> s.primaryState.clazz
                .equals(new DTGRARaw(f3, factory, val, standard).automaton.initialState.primaryState.clazz)));

        Formula f4 = Util.createFormula("(X a)  & a");
        assertTrue(SCC.get(3).stream().allMatch(s -> s.primaryState.clazz
                .equals(new DTGRARaw(f4, factory, val, standard).automaton.initialState.primaryState.clazz)));

        Formula f5 = Util.createFormula("a");
        assertTrue(SCC.get(2).stream().allMatch(s -> s.primaryState.clazz
                .equals(new DTGRARaw(f5, factory, val, standard).automaton.initialState.primaryState.clazz)));
    }

    @Test
    public void testIsSink1() {
        Formula formula = Util.createFormula("(X a) & (X X a) & (X X X a) & (X X X X a)");

        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(formula.getPropositions());
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(formula.getAtoms());

        DTGRARaw dtgra = new DTGRARaw(formula, factory, val, standard);
        List<Set<Product.ProductState>> SCC = dtgra.automaton.SCCs();

        assertTrue(dtgra.automaton.isSink(SCC.get(0)));
    }

    @Test
    public void testIsSink2() {
        Formula formula = Util.createFormula("(X a) & (X X a) & (X X X a) & (X X X X a)");

        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(formula.getPropositions());
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(formula.getAtoms());

        DTGRARaw dtgra = new DTGRARaw(formula, factory, val, standard);
        List<Set<Product.ProductState>> SCC = dtgra.automaton.SCCs();

        assertFalse(dtgra.automaton.isSink(SCC.get(5)));
    }

    @Test
    public void checkIfStatesGetLostInTheDTGRATranslation() {
        Formula f = Util.createFormula("true");
        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(f.getPropositions());
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(f.getAtoms());
        DTGRARaw dtgra = new DTGRARaw(f, factory, val, standard);
        assertEquals(dtgra.automaton.states.size(), 1);
        DTGRA dtg = new DTGRA(dtgra);
        assertEquals(dtg.states.size(), 1);

        dtgra = new DTGRARaw(f, factory, val, standard);
        assertEquals(dtgra.automaton.states.size(), 1);
        dtg = new DTGRA(dtgra);
        assertEquals(dtg.states.size(), 1);
    }

    @Test
    public void checkIfStatesGetLostInTheDTGRATranslation2() {
        Formula f = Util.createFormula("F a & G b");
        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(f.getPropositions());
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(f.getAtoms());
        DTGRARaw dtgra = new DTGRARaw(f, factory, val, standard);
        DTGRA dtg = new DTGRA(dtgra);
        assertFalse(dtg.states.isEmpty());

        dtgra = new DTGRARaw(f, factory, val, standard);
        assertEquals(dtgra.automaton.states.size(), 3);
        dtg = new DTGRA(dtgra);
        assertFalse(dtg.states.isEmpty());
    }

    @Test
    public void checkNullNotInTransition() {
        Formula f=Util.createFormula("p0");
        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(f.getPropositions());
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(f.getAtoms());
        DTGRARaw dtgra = new DTGRARaw(f, factory, val, standard);
        assertFalse(dtgra.automaton.transitions.values().contains(null));
        DTGRA dtg=new DTGRA(dtgra);
        assertFalse(dtg.transitions.values().contains(null));
    }

    @Test
    public void checkMissingState() {
        Formula f=Util.createFormula("p0");
        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(f.getPropositions());
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(f.getAtoms());
        DTGRARaw dtgra = new DTGRARaw(f, factory, val, standard);
        assertTrue(dtgra.automaton.states.size() == 3);
        DTGRA dtg=new DTGRA(dtgra);
        assertTrue(dtg.states.size() == 3);
    }

    @Test
    public void checkEmptinessCheck() {
        Formula f = Util.createFormula("G(p0)");
        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(f.getPropositions());
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(f.getAtoms());
        DTGRARaw dtgra = new DTGRARaw(f, factory, val, standard);
        assertFalse(dtgra.checkIfEmptyAndRemoveEmptySCCs());
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
        DTGRARaw dtgra = new DTGRARaw(f, factory, val, EnumSet.of(Optimisation.COMPUTE_ACC_CONDITION, Optimisation.NOT_ISABELLE_ACC));
        assertNotNull(dtgra);

        factory = FactoryRegistry.createEquivalenceClassFactory(Backend.BDD, f.getPropositions());
        val = FactoryRegistry.createValuationSetFactory(Backend.BDD, f.getAtoms());
        dtgra = new DTGRARaw(f, factory, val, EnumSet.of(Optimisation.COMPUTE_ACC_CONDITION, Optimisation.NOT_ISABELLE_ACC));
        assertNotNull(dtgra);

    }
}
