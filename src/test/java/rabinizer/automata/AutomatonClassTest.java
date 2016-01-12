package rabinizer.automata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rabinizer.exec.Main;
import rabinizer.ltl.EquivalenceClassFactory;
import rabinizer.ltl.FactoryRegistry;
import rabinizer.ltl.Formula;
import rabinizer.ltl.FormulaFactory;
import rabinizer.ltl.Util;
import rabinizer.ltl.ValuationSetFactory;

public class AutomatonClassTest {
    private boolean silent;

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
        Formula f1 = FormulaFactory.mkLit("p1", false);
        Formula f2 = FormulaFactory.mkLit("p1", false);
        Formula f3 = FormulaFactory.mkOr(f1, f2);
        Formula f4 = FormulaFactory.mkG(f3);

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

        DTGRARaw dtgra = new DTGRARaw(formula, true, false, false, false, false, false, equivalenceClassFactory,
                valuationSetFactory, true, false);
        assertNotNull(dtgra);
    }

    @Test
    public void testSCC1() {
        Formula formula = Util.createFormula("(p1&(p2|p3))");

        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(formula.getPropositions());
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(formula.getAtoms());

        DTGRARaw dtgra = new DTGRARaw(formula, true, false, false, false, false, false, factory, val, true, true);
        List<Set<Product.ProductState>> SCC = dtgra.automaton.SCCs();
        assertEquals(SCC.size(), 3);

    }

    @Test
    public void testSCC2() {
        Formula formula = Util.createFormula("(X a) & (X X a) & (X X X a) & (X X X X a)");

        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(formula.getPropositions());
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(formula.getAtoms());

        DTGRARaw dtgra = new DTGRARaw(formula, true, false, false, false, false, false, factory, val, true, true);
        List<Set<Product.ProductState>> SCC = dtgra.automaton.SCCs();
        assertEquals(SCC.size(), 7);

    }

    @Test
    public void testSCCtopSort1() {
        Formula formula = Util.createFormula("(p1&(p2|p3))");

        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(formula.getPropositions());
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(formula.getAtoms());

        DTGRARaw dtgra = new DTGRARaw(formula, true, false, false, false, false, false, factory, val, true, true);
        List<Set<Product.ProductState>> SCC = dtgra.automaton.SCCs();
        assertTrue(SCC.get(2).contains(dtgra.automaton.initialState));
    }

    @Test
    public void testSCCtopSort2() {
        Formula formula = Util.createFormula("(X a) & (X X a) & (X X X a) & (X X X X a)");

        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(formula.getPropositions());
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(formula.getAtoms());

        DTGRARaw dtgra = new DTGRARaw(formula, true, false, false, false, false, false, factory, val, true, true);
        List<Set<Product.ProductState>> SCC = dtgra.automaton.SCCs();

        DTGRARaw dtgra2 = new DTGRARaw(formula, true, false, false, false, false, false, factory, val, true, true);

        assertTrue(SCC.get(6).stream()
                .allMatch(s -> s.primaryState.clazz.equals(dtgra2.automaton.initialState.primaryState.clazz)));

        Formula f2 = Util.createFormula("(X a) & (X X a) & (X X X a) & a");
        assertTrue(SCC.get(5).stream().allMatch(s -> s.primaryState.clazz.equals(new DTGRARaw(f2, true, false, false,
                false, false, false, factory, val, true, false).automaton.initialState.primaryState.clazz)));

        Formula f3 = Util.createFormula("(X a) & (X X a)  & a");
        assertTrue(SCC.get(4).stream().allMatch(s -> s.primaryState.clazz.equals(new DTGRARaw(f3, true, false, false,
                false, false, false, factory, val, true, false).automaton.initialState.primaryState.clazz)));

        Formula f4 = Util.createFormula("(X a)  & a");
        assertTrue(SCC.get(3).stream().allMatch(s -> s.primaryState.clazz.equals(new DTGRARaw(f4, true, false, false,
                false, false, false, factory, val, true, false).automaton.initialState.primaryState.clazz)));

        Formula f5 = Util.createFormula("a");
        assertTrue(SCC.get(2).stream().allMatch(s -> s.primaryState.clazz.equals(new DTGRARaw(f5, true, false, false,
                false, false, false, factory, val, true, false).automaton.initialState.primaryState.clazz)));
    }

    @Test
    public void testIsSink1() {
        Formula formula = Util.createFormula("(X a) & (X X a) & (X X X a) & (X X X X a)");

        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(formula.getPropositions());
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(formula.getAtoms());

        DTGRARaw dtgra = new DTGRARaw(formula, true, false, false, false, false, false, factory, val, true, false);
        List<Set<Product.ProductState>> SCC = dtgra.automaton.SCCs();

        assertTrue(dtgra.automaton.isSink(SCC.get(0)));
    }

    @Test
    public void testIsSink2() {
        Formula formula = Util.createFormula("(X a) & (X X a) & (X X X a) & (X X X X a)");

        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(formula.getPropositions());
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(formula.getAtoms());

        DTGRARaw dtgra = new DTGRARaw(formula, true, false, false, false, false, false, factory, val, true, false);
        List<Set<Product.ProductState>> SCC = dtgra.automaton.SCCs();

        assertFalse(dtgra.automaton.isSink(SCC.get(5)));
    }

    @Test
    public void checkIfStatesGetLostInTheDTGRATranslation() {
        Formula f = Util.createFormula("true");
        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(f.getPropositions());
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(f.getAtoms());
        DTGRARaw dtgra = new DTGRARaw(f, true, false, false, false, false, false, factory, val, true, false);
        assertEquals(dtgra.automaton.states.size(), 1);
        DTGRA dtg = new DTGRA(dtgra);
        assertEquals(dtg.states.size(), 1);

        dtgra = new DTGRARaw(f, true, false, false, false, false, false, factory, val, true, false);
        assertEquals(dtgra.automaton.states.size(), 1);
        dtg = new DTGRA(dtgra);
        assertEquals(dtg.states.size(), 1);
    }

    @Test
    public void checkIfStatesGetLostInTheDTGRATranslation2() {
        Formula f = Util.createFormula("F a & G b");
        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(f.getPropositions());
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(f.getAtoms());
        DTGRARaw dtgra = new DTGRARaw(f, true, false, false, false, false, false, factory, val, true, false);
        DTGRA dtg = new DTGRA(dtgra);
        assertFalse(dtg.states.isEmpty());

        dtgra = new DTGRARaw(f, true, false, false, false, false, false, factory, val, true, false);
        assertEquals(dtgra.automaton.states.size(), 3);
        dtg = new DTGRA(dtgra);
        assertFalse(dtg.states.isEmpty());
    }

    @Test
    public void checkNullNotInTransition() {
        Formula f=Util.createFormula("p0");
        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(f.getPropositions());
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(f.getAtoms());
        DTGRARaw dtgra = new DTGRARaw(f, true, false, false, false, false, false, factory, val, true, false);
        assertFalse(dtgra.automaton.transitions.values().contains(null));
        DTGRA dtg=new DTGRA(dtgra);
        assertFalse(dtg.transitions.values().contains(null));
    }

    @Test
    public void checkMissingState() {
        Formula f=Util.createFormula("p0");
        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(f.getPropositions());
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(f.getAtoms());
        DTGRARaw dtgra = new DTGRARaw(f, true, false, false, false, false, false, factory, val, true, false);
        assertTrue(dtgra.automaton.states.size() == 3);
        DTGRA dtg=new DTGRA(dtgra);
        assertTrue(dtg.states.size() == 3);
    }

    @Test
    public void checkEmptinessCheck() {
        Formula f = Util.createFormula("G(p0)");
        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(f.getPropositions());
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(f.getAtoms());
        DTGRARaw dtgra = new DTGRARaw(f, true, false, false, false, false, false, factory, val, true, false);
        assertFalse(dtgra.checkIfEmptyAndRemoveEmptySCCs());
    }
}
