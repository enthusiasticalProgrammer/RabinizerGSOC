package rabinizer.automata;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import rabinizer.exec.Main;
import rabinizer.ltl.*;
import rabinizer.ltl.bdd.BDDEquivalenceClassFactory;
import rabinizer.ltl.bdd.BDDValuationSetFactory;
import rabinizer.ltl.z3.Z3EquivalenceClassFactory;
import rabinizer.ltl.z3.Z3ValuationSetFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

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

        BDDEquivalenceClassFactory factory = new BDDEquivalenceClassFactory(f4.getPropositions());
        BDDValuationSetFactory val = new BDDValuationSetFactory(f4.getAtoms());

        Master m = new Master(f4, factory, val, Collections.emptySet(), true);
        assertEquals(f4, m.generateInitialState().getClazz().getRepresentative());
    }

    @Test
    public void testDTGRARawConstructor() {
        Formula formula = Util.createFormula("(p1) U (X((G(F(G(p2)))) & (F(X(X(G(p2)))))))");

        EquivalenceClassFactory equivalenceClassFactory = new BDDEquivalenceClassFactory(formula.getPropositions());
        ValuationSetFactory valuationSetFactory = new BDDValuationSetFactory(formula.getAtoms());

        DTGRARaw dtgra = new DTGRARaw(formula, true, false, false, false, false, false, equivalenceClassFactory,
                valuationSetFactory);
    }

    @Test
    public void testSCC1() {
        Formula formula = Util.createFormula("(p1&(p2|p3))");

        BDDEquivalenceClassFactory factory = new BDDEquivalenceClassFactory(formula.getPropositions());
        BDDValuationSetFactory val = new BDDValuationSetFactory(formula.getAtoms());

        DTGRARaw dtgra = new DTGRARaw(formula, true, false, false, false, false, false, factory, val);
        dtgra.checkIfEmptyAndRemoveEmptySCCs();
        List<Set<Product.ProductState>> SCC = dtgra.automaton.SCCs();
        assertEquals(SCC.size(), 3);

    }

    @Test
    public void testSCC2() {
        Formula formula = Util.createFormula("(X a) & (X X a) & (X X X a) & (X X X X a)");

        BDDEquivalenceClassFactory factory = new BDDEquivalenceClassFactory(formula.getPropositions());
        BDDValuationSetFactory val = new BDDValuationSetFactory(formula.getAtoms());

        DTGRARaw dtgra = new DTGRARaw(formula, true, false, false, false, false, false, factory, val);
        dtgra.checkIfEmptyAndRemoveEmptySCCs();
        List<Set<Product.ProductState>> SCC = dtgra.automaton.SCCs();
        assertEquals(SCC.size(), 7);

    }

    @Test
    public void testSCCtopSort1() {
        Formula formula = Util.createFormula("(p1&(p2|p3))");

        BDDEquivalenceClassFactory factory = new BDDEquivalenceClassFactory(formula.getPropositions());
        BDDValuationSetFactory val = new BDDValuationSetFactory(formula.getAtoms());

        DTGRARaw dtgra = new DTGRARaw(formula, true, false, false, false, false, false, factory, val);
        dtgra.checkIfEmptyAndRemoveEmptySCCs();
        List<Set<Product.ProductState>> SCC = dtgra.automaton.SCCs();
        assertTrue(SCC.get(2).contains(dtgra.automaton.initialState));
    }

    @Test
    public void testSCCtopSort2() {
        Formula formula = Util.createFormula("(X a) & (X X a) & (X X X a) & (X X X X a)");

        BDDEquivalenceClassFactory factory = new BDDEquivalenceClassFactory(formula.getPropositions());
        BDDValuationSetFactory val = new BDDValuationSetFactory(formula.getAtoms());

        DTGRARaw dtgra = new DTGRARaw(formula, true, false, false, false, false, false, factory, val);
        dtgra.checkIfEmptyAndRemoveEmptySCCs();
        List<Set<Product.ProductState>> SCC = dtgra.automaton.SCCs();

        DTGRARaw dtgra2 = new DTGRARaw(formula, true, false, false, false, false, false, factory, val);
        dtgra2.checkIfEmptyAndRemoveEmptySCCs();

        assertTrue(SCC.get(6).stream()
                .allMatch(s -> s.primaryState.clazz.equals(dtgra2.automaton.initialState.primaryState.clazz)));

        Formula f2 = Util.createFormula("(X a) & (X X a) & (X X X a) & a");
        assertTrue(SCC.get(5).stream().allMatch(s -> s.primaryState.clazz.equals(new DTGRARaw(f2, true, false, false,
                false, false, false, factory, val).automaton.initialState.primaryState.clazz)));

        Formula f3 = Util.createFormula("(X a) & (X X a)  & a");
        assertTrue(SCC.get(4).stream().allMatch(s -> s.primaryState.clazz.equals(new DTGRARaw(f3, true, false, false,
                false, false, false, factory, val).automaton.initialState.primaryState.clazz)));

        Formula f4 = Util.createFormula("(X a)  & a");
        assertTrue(SCC.get(3).stream().allMatch(s -> s.primaryState.clazz.equals(new DTGRARaw(f4, true, false, false,
                false, false, false, factory, val).automaton.initialState.primaryState.clazz)));

        Formula f5 = Util.createFormula("a");
        assertTrue(SCC.get(2).stream().allMatch(s -> s.primaryState.clazz.equals(new DTGRARaw(f5, true, false, false,
                false, false, false, factory, val).automaton.initialState.primaryState.clazz)));
    }

    @Test
    public void testIsSink1() {
        Formula formula = Util.createFormula("(X a) & (X X a) & (X X X a) & (X X X X a)");

        BDDEquivalenceClassFactory factory = new BDDEquivalenceClassFactory(formula.getPropositions());
        BDDValuationSetFactory val = new BDDValuationSetFactory(formula.getAtoms());

        DTGRARaw dtgra = new DTGRARaw(formula, true, false, false, false, false, false, factory, val);
        List<Set<Product.ProductState>> SCC = dtgra.automaton.SCCs();

        assertTrue(dtgra.automaton.isSink(SCC.get(0)));
    }

    @Test
    public void testIsSink2() {
        Formula formula = Util.createFormula("(X a) & (X X a) & (X X X a) & (X X X X a)");

        BDDEquivalenceClassFactory factory = new BDDEquivalenceClassFactory(formula.getPropositions());
        BDDValuationSetFactory val = new BDDValuationSetFactory(formula.getAtoms());

        DTGRARaw dtgra = new DTGRARaw(formula, true, false, false, false, false, false, factory, val);
        List<Set<Product.ProductState>> SCC = dtgra.automaton.SCCs();

        assertFalse(dtgra.automaton.isSink(SCC.get(5)));
    }

    @Test
    public void checkIfStatesGetLostInTheDTGRATranslation() {
        Formula f = Util.createFormula("true");
        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(f.getPropositions());
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(f.getAtoms());
        DTGRARaw dtgra = new DTGRARaw(f, true, false, false, false, false, false, factory, val);
        assertEquals(dtgra.automaton.states.size(), 1);
        DTGRA dtg = new DTGRA(dtgra);
        assertEquals(dtg.states.size(), 1);

        factory = new BDDEquivalenceClassFactory(f.getPropositions());
        val = new BDDValuationSetFactory(f.getAtoms());
        dtgra = new DTGRARaw(f, true, false, false, false, false, false, factory, val);
        assertEquals(dtgra.automaton.states.size(), 1);
        dtg = new DTGRA(dtgra);
        assertEquals(dtg.states.size(), 1);
    }

    @Test
    public void checkIfStatesGetLostInTheDTGRATranslation2() {
        Formula f = Util.createFormula("F a & G b");
        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(f.getPropositions());
        ValuationSetFactory val = FactoryRegistry.createValuationSetFactory(f.getAtoms());
        DTGRARaw dtgra = new DTGRARaw(f, true, false, false, false, false, false, factory, val);
        DTGRA dtg = new DTGRA(dtgra);
        assertFalse(dtg.states.isEmpty());

        factory = new BDDEquivalenceClassFactory(f.getPropositions());
        val = new BDDValuationSetFactory(f.getAtoms());
        dtgra = new DTGRARaw(f, true, false, false, false, false, false, factory, val);
        assertEquals(dtgra.automaton.states.size(), 1);
        dtg = new DTGRA(dtgra);
        assertFalse(dtg.states.isEmpty());
    }
}
