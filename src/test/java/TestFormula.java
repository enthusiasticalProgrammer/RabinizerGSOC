import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import rabinizer.ltl.bdd.BDDForVariables;
import rabinizer.ltl.bdd.Valuation;
import rabinizer.ltl.Disjunction;
import rabinizer.ltl.Formula;
import rabinizer.ltl.FormulaFactory;
import rabinizer.ltl.ImplicationVisitor;
import rabinizer.ltl.Literal;
import rabinizer.ltl.SimplifyAggressivelyVisitor;

public class TestFormula {
	public void setUp() {
		System.out.print("Testing...");

	}

	public void tearDown() {
		System.out.println("Testing done");
	}

	@Test
	public void testFormulaEquality() {
		BDDForVariables.init();
		Formula f1 = FormulaFactory.mkLit("p1", BDDForVariables.bijectionIdAtom.id("p1"), false);
		Formula f2 = FormulaFactory.mkLit("p1", BDDForVariables.bijectionIdAtom.id("p1"), false);
		assertEquals(f1, f2);
	}

	@Test
	public void testFormulaEquality2() {
		BDDForVariables.init();
		Formula f1 = FormulaFactory.mkLit("p1", BDDForVariables.bijectionIdAtom.id("p1"), false);
		Formula f2 = FormulaFactory.mkLit("p1", BDDForVariables.bijectionIdAtom.id("p1"), true);
		assertEquals(!f1.equals(f2), true);
	}

	@Test
	public void testFormulaEquality3() {
		BDDForVariables.init();
		Formula f1 = FormulaFactory.mkLit("p1", BDDForVariables.bijectionIdAtom.id("p1"), false);
		Formula f2 = FormulaFactory.mkLit("p2", BDDForVariables.bijectionIdAtom.id("p2"), false);
		assertEquals(!f1.equals(f2), true);
	}

	@Test
	public void testFormulaFactory1() {
		BDDForVariables.init();
		Formula f1 = FormulaFactory.mkLit("p1", BDDForVariables.bijectionIdAtom.id("p1"), false);
		Formula f2 = FormulaFactory.mkLit("p1", BDDForVariables.bijectionIdAtom.id("p1"), false);
		Formula f3 = FormulaFactory.mkOr(f1, f2);
		assertEquals(f3, f2);
	}

	@Test
	public void testFormulaFactory2() {
		BDDForVariables.init();
		Formula f1 = FormulaFactory.mkLit("p1", BDDForVariables.bijectionIdAtom.id("p1"), false);
		Formula f2 = FormulaFactory.mkLit("p2", BDDForVariables.bijectionIdAtom.id("p2"), false);
		Formula f3 = FormulaFactory.mkOr(f1, f2);
		Formula f4 = FormulaFactory.mkOr(f1, f3);
		assertEquals(f3, f4);
	}

	@Test
	public void testFormulaFactory3() {
		BDDForVariables.init();
		Formula f1 = FormulaFactory.mkLit("p1", BDDForVariables.bijectionIdAtom.id("p1"), false);
		Formula f2 = FormulaFactory.mkLit("p1", BDDForVariables.bijectionIdAtom.id("p1"), false);
		Formula f3 = FormulaFactory.mkAnd(f1, f2);
		assertEquals(f3, f2);
	}

	@Test
	public void testFormulaFactory4() {
		BDDForVariables.init();
		Formula f1 = FormulaFactory.mkLit("p1", BDDForVariables.bijectionIdAtom.id("p1"), false);
		Formula f2 = FormulaFactory.mkLit("p2", BDDForVariables.bijectionIdAtom.id("p2"), false);
		Formula f3 = FormulaFactory.mkAnd(f1, f2);
		Formula f4 = FormulaFactory.mkAnd(f1, f3);
		assertEquals(f3, f4);
	}

	@Test
	public void testFormulaFactory5() {
		Formula f0 = FormulaFactory.mkConst(false);
		Formula f1 = FormulaFactory.mkConst(false);
		Formula f2 = FormulaFactory.mkConst(false);
		Formula f3 = FormulaFactory.mkOr(f0, f1, f2);
		assertEquals(f3, FormulaFactory.mkConst(false));
	}

	@Test
	public void testFormulaFactory6() {
		BDDForVariables.init();
		Formula f1 = FormulaFactory.mkLit("p1", BDDForVariables.bijectionIdAtom.id("p1"), false);
		Formula f2 = FormulaFactory.mkLit("p2", BDDForVariables.bijectionIdAtom.id("p2"), false);
		Formula f3 = FormulaFactory.mkF(f1);
		Formula f4 = FormulaFactory.mkU(f3, f2);

		Formula f5 = FormulaFactory.mkOr(f2, FormulaFactory.mkF(FormulaFactory.mkAnd(FormulaFactory.mkX(f2), f3)));
		assertNotEquals(f4, f5);
	}

	@Test
	public void testFormulaFactory7() {
		BDDForVariables.init();
		Formula f1 = FormulaFactory.mkLit("p1", BDDForVariables.bijectionIdAtom.id("p1"), false);
		Formula f2 = FormulaFactory.mkX(f1);
		Formula f3 = FormulaFactory.mkF(f2);
		assertNotEquals(f3.toString(), "XFp1");
	}

	@Test
	public void unique1() {
		BDDForVariables.init();
		Formula f0 = FormulaFactory.mkLit("p0", BDDForVariables.bijectionIdAtom.id("p0"), false);
		Formula f1 = FormulaFactory.mkLit("p1", BDDForVariables.bijectionIdAtom.id("p1"), false);
		Formula f2 = FormulaFactory.mkLit("p2", BDDForVariables.bijectionIdAtom.id("p2"), false);
		Formula f3 = FormulaFactory.mkAnd(FormulaFactory.mkOr(f1, f2), f1);
		assertTrue(f0.get_id() != f1.get_id());
		assertTrue(f0.get_id() != f2.get_id());
		assertTrue(f0.get_id() != f3.get_id());
		assertTrue(f1.get_id() != f2.get_id());
		assertTrue(f2.get_id() != f3.get_id());

	}

	@Test
	public void simplify1() {
		BDDForVariables.init();
		Formula f1 = FormulaFactory.mkLit("p1", BDDForVariables.bijectionIdAtom.id("p1"), false);
		Formula f2 = FormulaFactory.mkLit("p2", BDDForVariables.bijectionIdAtom.id("p2"), false);
		Formula f3 = FormulaFactory.mkOr(f1, f2);
		assertTrue(f3 instanceof Disjunction);

	}

	@Test
	public void simplify2() {
		BDDForVariables.init();
		Formula f0 = FormulaFactory.mkLit("p0", BDDForVariables.bijectionIdAtom.id("p0"), false);
		Formula f1 = FormulaFactory.mkLit("p1", BDDForVariables.bijectionIdAtom.id("p1"), false);
		Formula f2 = FormulaFactory.mkLit("p2", BDDForVariables.bijectionIdAtom.id("p2"), false);
		Formula f3 = FormulaFactory.mkNot(FormulaFactory.mkAnd(FormulaFactory.mkOr(f1, f2), f0));

		Formula f4 = FormulaFactory.mkLit("p0", BDDForVariables.bijectionIdAtom.id("p0"), true);
		Formula f5 = FormulaFactory.mkLit("p1", BDDForVariables.bijectionIdAtom.id("p1"), true);
		Formula f6 = FormulaFactory.mkLit("p2", BDDForVariables.bijectionIdAtom.id("p2"), true);
		Formula f7 = FormulaFactory.mkOr(f4, FormulaFactory.mkAnd(f5, f6));
		assertEquals(f3, f7);

	}

	@Test
	public void simplify3() {
		BDDForVariables.init();
		Formula f0 = FormulaFactory.mkLit("p0", BDDForVariables.bijectionIdAtom.id("p0"), false);
		Formula f1 = FormulaFactory.mkU(FormulaFactory.mkConst(true), f0);
		Formula f2 = FormulaFactory.mkAnd(f0, f1);
		Formula f3 = FormulaFactory.mkNot(f2);
		assertNotEquals(f3, FormulaFactory.mkNot(f0));
	}

	@Test
	public void simplify4() {
		BDDForVariables.init();
		Formula f0 = FormulaFactory.mkLit("p0", BDDForVariables.bijectionIdAtom.id("p0"), false);
		Formula f1 = FormulaFactory.mkLit("p1", BDDForVariables.bijectionIdAtom.id("p1"), false);
		Formula f2 = FormulaFactory.mkU(f0, f1);
		Formula f3 = FormulaFactory.mkNot(f2);

		Formula f4 = FormulaFactory.mkLit("p0", BDDForVariables.bijectionIdAtom.id("p0"), true);
		Formula f5 = FormulaFactory.mkLit("p1", BDDForVariables.bijectionIdAtom.id("p1"), true);
		Formula f6 = FormulaFactory.mkU(f5, FormulaFactory.mkAnd(f4, f5));
		Formula f7 = FormulaFactory.mkOr(FormulaFactory.mkG(f5), f6);

		assertNotEquals(f3, f7);
	}

	@Test
	public void simplify5() {
		BDDForVariables.init();
		Formula f0 = FormulaFactory.mkLit("p0", BDDForVariables.bijectionIdAtom.id("p0"), false);
		Formula f1 = FormulaFactory.mkLit("p1", BDDForVariables.bijectionIdAtom.id("p1"), false);
		Formula f2 = FormulaFactory.mkG(FormulaFactory.mkU(f0, f1));
		Formula f3 = FormulaFactory.mkNot(f2);

		Formula f4 = FormulaFactory.mkLit("p0", BDDForVariables.bijectionIdAtom.id("p0"), true);
		Formula f5 = FormulaFactory.mkLit("p1", BDDForVariables.bijectionIdAtom.id("p1"), true);
		Formula f6 = FormulaFactory.mkU(f5, FormulaFactory.mkAnd(f4, f5));
		Formula f7 = FormulaFactory.mkF(FormulaFactory.mkOr(FormulaFactory.mkG(f5), f6));

		assertNotEquals(f3, f7);
	}

	@Test
	public void testMkOr() {
		BDDForVariables.init();
		Formula f0 = FormulaFactory.mkLit("p0", BDDForVariables.bijectionIdAtom.id("p0"), false);
		Formula f1 = FormulaFactory.mkLit("p1", BDDForVariables.bijectionIdAtom.id("p1"), false);
		Formula f2 = FormulaFactory.mkOr(f0, f1);
		assertTrue(f2.toString().equals("(p0|p1)") || f2.toString().equals("(p1|p0)"));
	}

	@Test
	public void testSetToConst1() {
		BDDForVariables.init();
		Formula f0 = FormulaFactory.mkLit("p0", BDDForVariables.bijectionIdAtom.id("p0"), false);
		Formula f1 = f0.setToConst(f0.get_id(), true);
		assertEquals(f1, FormulaFactory.mkConst(true));
	}

	@Test
	public void testSetToConst2() {
		BDDForVariables.init();
		Formula f1 = FormulaFactory.mkLit("p1", BDDForVariables.bijectionIdAtom.id("p1"), false);
		Formula f2 = FormulaFactory.mkLit("p2", BDDForVariables.bijectionIdAtom.id("p2"), false);
		Formula f3 = FormulaFactory.mkOr(f1, f2);
		assertEquals(f3.setToConst(f2.get_id(), false), f1);
	}

	@Test
	public void testUnfold1() {
		BDDForVariables.init();
		Formula f0 = FormulaFactory.mkLit("p0", BDDForVariables.bijectionIdAtom.id("p0"), false);
		Formula f1 = FormulaFactory.mkLit("p1", BDDForVariables.bijectionIdAtom.id("p1"), false);
		Formula f2 = FormulaFactory.mkU(f0, f1);
		Formula f3 = f2.unfold();
		Formula f4 = FormulaFactory.mkOr(f1, FormulaFactory.mkAnd(f0, f2));
		assertEquals(f3, f4);
	}

	@Test
	public void testAssertValuation1() {
		BDDForVariables.init();
		Formula f1 = FormulaFactory.mkLit("p2", BDDForVariables.bijectionIdAtom.id("p2"), true);
		Formula f2 = FormulaFactory.mkG(f1);
		Formula f3 = FormulaFactory.mkAnd(f2, f1);
		Valuation v = new Valuation();
		v.set(BDDForVariables.bijectionIdAtom.id("p2"), true);
		assertEquals(f3.assertValuation(v), FormulaFactory.mkConst(false));
	}

	@Test
	public void testAssertValuation2() {
		BDDForVariables.init();
		Formula f0 = FormulaFactory.mkLit("p0", BDDForVariables.bijectionIdAtom.id("p0"), true);
		Formula f1 = FormulaFactory.mkLit("p2", BDDForVariables.bijectionIdAtom.id("p2"), true);
		Formula f2 = FormulaFactory.mkAnd(f0, f1);
		Formula f3 = FormulaFactory.mkU(f1, f2);
		Formula f4 = FormulaFactory.mkG(f1);
		Formula f5 = FormulaFactory.mkAnd(f1, f4);
		Formula f6 = FormulaFactory.mkAnd(f1, f3);
		Formula f7 = FormulaFactory.mkOr(f2, f5, f6);
		Valuation v = new Valuation();
		v.set(BDDForVariables.bijectionIdAtom.id("p2"), true);
		v.set(BDDForVariables.bijectionIdAtom.id("p0"), false);
		assertEquals(f7.assertValuation(v), FormulaFactory.mkConst(false));
	}

	@Test
	public void testAssertValuation3() {
		BDDForVariables.init();
		Formula f1 = FormulaFactory.mkLit("p2", BDDForVariables.bijectionIdAtom.id("p2"), true);
		Formula f4 = FormulaFactory.mkG(f1);
		Formula f5 = f4.unfold();
		Valuation v = new Valuation();
		v.set(BDDForVariables.bijectionIdAtom.id("p2"), true);
		v.set(BDDForVariables.bijectionIdAtom.id("p0"), false);

		assertEquals(f5.temporalStep(v), FormulaFactory.mkConst(false));
	}

	@Test
	public void testAssertValuation4() {
		BDDForVariables.init();
		Formula f1 = FormulaFactory.mkLit("p0", BDDForVariables.bijectionIdAtom.id("p0"), false);
		Formula f2 = FormulaFactory.mkLit("p1", BDDForVariables.bijectionIdAtom.id("p1"), false);
		Formula f3 = FormulaFactory.mkLit("p2", BDDForVariables.bijectionIdAtom.id("p2"), false);
		Formula f4 = FormulaFactory.mkAnd(f1, f2, f3);
		assertEquals(f4.assertLiteral((Literal) f1), FormulaFactory.mkAnd(f2, f3));
	}

	@Test
	public void gSubformulas() {
		BDDForVariables.init();

		Formula f1 = FormulaFactory.mkLit("p1", BDDForVariables.bijectionIdAtom.id("p1"), false);
		Formula f2 = FormulaFactory.mkF(FormulaFactory.mkG(f1));
		Set<Formula> test = new HashSet<Formula>();
		test.add(f1);

		assertEquals(test, f2.gSubformulas());
	}

	@Test
	public void testSimplifyForEntails1() {
		BDDForVariables.init();
		Formula f1 = FormulaFactory.mkLit("p0", BDDForVariables.bijectionIdAtom.id("p0"), false);
		Formula f2 = FormulaFactory.mkLit("p1", BDDForVariables.bijectionIdAtom.id("p1"), false);
		Formula f3 = FormulaFactory.mkLit("p2", BDDForVariables.bijectionIdAtom.id("p2"), false);

		Formula f4 = FormulaFactory.mkG(f1);
		Formula f5 = FormulaFactory.mkG(FormulaFactory.mkF(f1));
		Formula f6 = FormulaFactory.mkAnd(f4, f5);
		assertNotEquals(f6, FormulaFactory.mkG(FormulaFactory.mkAnd(f1, FormulaFactory.mkF(f1))));
	}

	@Test
	public void testSimplifyForEntails2() {
		BDDForVariables.init();
		Formula f1 = FormulaFactory.mkLit("p0", BDDForVariables.bijectionIdAtom.id("p0"), false);
		Formula f2 = FormulaFactory.mkLit("p1", BDDForVariables.bijectionIdAtom.id("p1"), false);
		Formula f3 = FormulaFactory.mkLit("p2", BDDForVariables.bijectionIdAtom.id("p2"), false);

		Formula f4 = FormulaFactory.mkX(f1);
		Formula f5 = FormulaFactory.mkX(FormulaFactory.mkF(f1));
		Formula f6 = FormulaFactory.mkOr(f4, f5);
		assertNotEquals(f6, FormulaFactory.mkX(FormulaFactory.mkOr(f1, FormulaFactory.mkF(f1))));
	}

	@Test
	public void testUnfold2() {
		Formula f1 = FormulaFactory.mkLit("p0", BDDForVariables.bijectionIdAtom.id("p0"), false);
		Formula f2 = FormulaFactory.mkLit("p1", BDDForVariables.bijectionIdAtom.id("p1"), false);
		Formula f3 = FormulaFactory.mkG(f1);
		Formula f4 = FormulaFactory.mkG(f2);
		Formula f5 = FormulaFactory.mkAnd(f3, f4);
		Formula f6 = FormulaFactory.mkAnd(f5, f1, f2);
		assertEquals(f6, f5.unfold());
	}

	@Test
	public void testImplication1() {
		Formula f1 = FormulaFactory.mkLit("p0", BDDForVariables.bijectionIdAtom.id("p0"), false);
		Formula f2 = FormulaFactory.mkG(f1);
		Formula f3 = FormulaFactory.mkX(f1);
		Formula f4 = FormulaFactory.mkG(FormulaFactory.mkF(f3));
		Formula f5 = FormulaFactory.mkAnd(f4, f2);
		ImplicationVisitor v = ImplicationVisitor.getVisitor();
		assertEquals(f2.acceptBinarybool(v, f5), true);
	}

	@Test
	public void testSimplifyAggressively1() {
		Formula f1 = FormulaFactory.mkLit("p0", BDDForVariables.bijectionIdAtom.id("p0"), false);
		Formula f2 = FormulaFactory.mkG(FormulaFactory.mkF(f1));
		Formula f3 = FormulaFactory.mkX(f1);
		Formula f4 = FormulaFactory.mkG(FormulaFactory.mkF(f3));
		SimplifyAggressivelyVisitor v = SimplifyAggressivelyVisitor.getVisitor();
		assertEquals(f4.acceptFormula(v), f2);
	}

	@Test
	public void testSimplifyAggressively2() {
		Formula f1 = FormulaFactory.mkLit("p0", BDDForVariables.bijectionIdAtom.id("p0"), false);
		Formula f2 = FormulaFactory.mkG(f1);
		Formula f3 = FormulaFactory.mkX(f1);
		Formula f4 = FormulaFactory.mkG(FormulaFactory.mkF(f3));
		Formula f5 = FormulaFactory.mkAnd(f4, f2);
		SimplifyAggressivelyVisitor v = SimplifyAggressivelyVisitor.getVisitor();
		assertEquals(f5.acceptFormula(v), f2);
	}

	@Test
	public void testSimplifyAggressively3() {
		Formula f1 = FormulaFactory.mkLit("p0", BDDForVariables.bijectionIdAtom.id("p0"), false);
		Formula f2 = FormulaFactory.mkF(FormulaFactory.mkConst(true));
		Formula f3 = FormulaFactory.mkAnd(f1, f2);

		SimplifyAggressivelyVisitor v = SimplifyAggressivelyVisitor.getVisitor();
		assertEquals(f3.acceptFormula(v), f1);
	}

	@Test
	public void testSimplifyAggressively4() {
		Formula f1 = FormulaFactory.mkLit("p0", BDDForVariables.bijectionIdAtom.id("p0"), false);
		Formula f2 = FormulaFactory.mkU(f1, f1);

		SimplifyAggressivelyVisitor v = SimplifyAggressivelyVisitor.getVisitor();
		assertEquals(f2.acceptFormula(v), f1);
	}

	@Test
	public void test_simplify_boolean1() {
		Formula f1 = FormulaFactory.mkLit("p0", BDDForVariables.bijectionIdAtom.id("p0"), false);
		Formula f2 = FormulaFactory.mkConst(true);
		Formula f3 = FormulaFactory.mkAnd(f1, f2);
		assertEquals(f3, f1);
	}

	@Test
	public void test_setConst() {
		Formula f1 = FormulaFactory.mkLit("p0", BDDForVariables.bijectionIdAtom.id("p0"), false);
		Formula f2 = FormulaFactory.mkLit("p1", BDDForVariables.bijectionIdAtom.id("p1"), false);
		Formula f3 = FormulaFactory.mkF(FormulaFactory.mkAnd(f1, f2));
		assertEquals(f3.setToConst(f1.get_id(), true), f3);
	}

}
