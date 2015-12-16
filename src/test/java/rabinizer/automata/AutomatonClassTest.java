package rabinizer.automata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.util.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Table.Cell;

import rabinizer.automata.MasterFolded;
import rabinizer.exec.Main;
import rabinizer.parser.LTLParser;
import rabinizer.parser.ParseException;
import rabinizer.ltl.EquivalenceClassFactory;
import rabinizer.ltl.Formula;
import rabinizer.ltl.FormulaFactory;
import rabinizer.ltl.Util;
import rabinizer.ltl.ValuationSet;
import rabinizer.ltl.bdd.BDDEquivalenceClassFactory;
import rabinizer.ltl.bdd.BDDValuationSetFactory;
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

        BDDEquivalenceClassFactory factory = new BDDEquivalenceClassFactory(f4.getPropositions());
        BDDValuationSetFactory val = new BDDValuationSetFactory(f4.getAtoms());

        MasterFolded m = new MasterFolded(f4, factory, val);
        assertEquals(f4, m.getFormula());
    }

    @Test
    public void testDTGRARawConstructor() {
        Formula formula = Util.createFormula("(p1) U (X((G(F(G(p2)))) & (F(X(X(G(p2)))))))");

        EquivalenceClassFactory equivalenceClassFactory = new BDDEquivalenceClassFactory(formula.getPropositions());
        ValuationSetFactory<String> valuationSetFactory = new BDDValuationSetFactory(formula.getAtoms());

        DTGRARaw dtgra = new DTGRARaw(formula, true, false, false, false, false, false, equivalenceClassFactory,
                valuationSetFactory);
    }

    @Test
    public void testSCC1() {

        Formula formula = Util.createFormula("(p1) U (X((G(F(G(p2)))) & (F(X(X(G(p2)))))))");
        BDDEquivalenceClassFactory factory = new BDDEquivalenceClassFactory(formula.getPropositions());
        BDDValuationSetFactory val = new BDDValuationSetFactory(formula.getAtoms());

        DTGRARaw dtgra = new DTGRARaw(formula, true, false, false, false, false, false, factory, val);
        List<Set<ProductState>> SCC = dtgra.automaton.SCCs();
        assertEquals(SCC.size(), 31);

    }

    @Test
    public void testSCC2() {
        Formula formula = Util.createFormula("(p1&(p2|p3))");

        BDDEquivalenceClassFactory factory = new BDDEquivalenceClassFactory(formula.getPropositions());
        BDDValuationSetFactory val = new BDDValuationSetFactory(formula.getAtoms());

        DTGRARaw dtgra = new DTGRARaw(formula, true, false, false, false, false, false, factory, val);
        List<Set<ProductState>> SCC = dtgra.automaton.SCCs();
        assertEquals(SCC.size(), 3);

    }

    @Test
    public void testSCC3() {
        Formula formula = Util.createFormula("(X a) & (X X a) & (X X X a) & (X X X X a)");

        BDDEquivalenceClassFactory factory = new BDDEquivalenceClassFactory(formula.getPropositions());
        BDDValuationSetFactory val = new BDDValuationSetFactory(formula.getAtoms());

        DTGRARaw dtgra = new DTGRARaw(formula, true, false, false, false, false, false, factory, val);
        List<Set<ProductState>> SCC = dtgra.automaton.SCCs();
        assertEquals(SCC.size(), 7);

    }

    @Test
    public void testSCCtopSort1() {
        Formula formula = Util.createFormula("(p1) U (X((G(F(G(p2)))) & (F(X(X(G(p2)))))))");

        BDDEquivalenceClassFactory factory = new BDDEquivalenceClassFactory(formula.getPropositions());
        BDDValuationSetFactory val = new BDDValuationSetFactory(formula.getAtoms());

        DTGRARaw dtgra = new DTGRARaw(formula, true, false, false, false, false, false, factory, val);
        List<Set<ProductState>> SCC = dtgra.automaton.SCCs();
        assertTrue(SCC.get(30).contains(dtgra.automaton.initialState));
    }

    @Test
    public void testSCCtopSort2() {
        Formula formula = Util.createFormula("(p1&(p2|p3))");

        BDDEquivalenceClassFactory factory = new BDDEquivalenceClassFactory(formula.getPropositions());
        BDDValuationSetFactory val = new BDDValuationSetFactory(formula.getAtoms());

        DTGRARaw dtgra = new DTGRARaw(formula, true, false, false, false, false, false, factory, val);
        List<Set<ProductState>> SCC = dtgra.automaton.SCCs();
        assertTrue(SCC.get(2).contains(dtgra.automaton.initialState));
    }

    @Test
    public void testSCCtopSort3() {
        Formula formula = Util.createFormula("(X a) & (X X a) & (X X X a) & (X X X X a)");

        BDDEquivalenceClassFactory factory = new BDDEquivalenceClassFactory(formula.getPropositions());
        BDDValuationSetFactory val = new BDDValuationSetFactory(formula.getAtoms());

        DTGRARaw dtgra = new DTGRARaw(formula, true, false, false, false, false, false, factory, val);
        List<Set<ProductState>> SCC = dtgra.automaton.SCCs();

        assertTrue(SCC.get(6).stream().allMatch(s -> s.equals(
                new DTGRARaw(formula, true, false, false, false, false, false, factory, val).automaton.initialState)));

        Formula f2 = Util.createFormula("(X a) & (X X a) & (X X X a) & a");
        assertTrue(SCC.get(5).stream().allMatch(s -> s.equals(
                new DTGRARaw(f2, true, false, false, false, false, false, factory, val).automaton.initialState)));

        Formula f3 = Util.createFormula("(X a) & (X X a)  & a");
        assertTrue(SCC.get(4).stream().allMatch(s -> s.equals(
                new DTGRARaw(f3, true, false, false, false, false, false, factory, val).automaton.initialState)));

        Formula f4 = Util.createFormula("(X a)  & a");
        assertTrue(SCC.get(3).stream().allMatch(s -> s.equals(
                new DTGRARaw(f4, true, false, false, false, false, false, factory, val).automaton.initialState)));

        Formula f5 = Util.createFormula("a");
        assertTrue(SCC.get(2).stream().allMatch(s -> s.equals(
                new DTGRARaw(f5, true, false, false, false, false, false, factory, val).automaton.initialState)));

        Formula f = Util.createFormula("false");
        Formula t = Util.createFormula("true");

        ProductState fal = new DTGRARaw(f, true, false, false, false, false, false, factory,
                val).automaton.initialState;
        ProductState tru = new DTGRARaw(t, true, false, false, false, false, false, factory,
                val).automaton.initialState;

        assertTrue(
                (SCC.get(1).stream().allMatch(s -> s.equals(fal)) && SCC.get(0).stream().allMatch(s -> s.equals(tru)))
                        ^ (SCC.get(1).stream().allMatch(s -> s.equals(tru)))
                        && SCC.get(0).stream().allMatch(s -> s.equals(fal)));
    }

    @Test
    public void testIsSink1() {
        Formula formula = Util.createFormula("(X a) & (X X a) & (X X X a) & (X X X X a)");

        BDDEquivalenceClassFactory factory = new BDDEquivalenceClassFactory(formula.getPropositions());
        BDDValuationSetFactory val = new BDDValuationSetFactory(formula.getAtoms());

        DTGRARaw dtgra = new DTGRARaw(formula, true, false, false, false, false, false, factory, val);
        List<Set<ProductState>> SCC = dtgra.automaton.SCCs();

        assertTrue(dtgra.automaton.isSink(SCC.get(0)));
    }

    @Test
    public void testIsSink2() {
        Formula formula = Util.createFormula("(X a) & (X X a) & (X X X a) & (X X X X a)");

        BDDEquivalenceClassFactory factory = new BDDEquivalenceClassFactory(formula.getPropositions());
        BDDValuationSetFactory val = new BDDValuationSetFactory(formula.getAtoms());

        DTGRARaw dtgra = new DTGRARaw(formula, true, false, false, false, false, false, factory, val);
        List<Set<ProductState>> SCC = dtgra.automaton.SCCs();

        assertFalse(dtgra.automaton.isSink(SCC.get(5)));
    }
}
