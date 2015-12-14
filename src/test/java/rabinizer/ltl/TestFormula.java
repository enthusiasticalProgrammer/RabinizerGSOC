package rabinizer.ltl;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class TestFormula {

    @Test
    public void testFormulaEquality() {
        Formula f1 = FormulaFactory.mkLit("p1", false);
        Formula f2 = FormulaFactory.mkLit("p1", false);
        assertEquals(f1, f2);
    }

    @Test
    public void testFormulaEquality2() {
        Formula f1 = FormulaFactory.mkLit("p1", false);
        Formula f2 = FormulaFactory.mkLit("p1", true);
        assertEquals(!f1.equals(f2), true);
    }

    @Test
    public void testFormulaEquality3() {
        Formula f1 = FormulaFactory.mkLit("p1", false);
        Formula f2 = FormulaFactory.mkLit("p2", false);
        assertEquals(!f1.equals(f2), true);
    }

    @Test
    public void testFormulaFactory1() {
        Formula f1 = FormulaFactory.mkLit("p1", false);
        Formula f2 = FormulaFactory.mkLit("p1", false);
        Formula f3 = FormulaFactory.mkOr(f1, f2);
        assertEquals(f3, f2);
    }

    @Test
    public void testFormulaFactory2() {
        Formula f1 = FormulaFactory.mkLit("p1", false);
        Formula f2 = FormulaFactory.mkLit("p2", false);
        Formula f3 = FormulaFactory.mkOr(f1, f2);
        Formula f4 = FormulaFactory.mkOr(f1, f3);
        assertEquals(f3, f4);
    }

    @Test
    public void testFormulaFactory3() {
        Formula f1 = FormulaFactory.mkLit("p1", false);
        Formula f2 = FormulaFactory.mkLit("p1", false);
        Formula f3 = FormulaFactory.mkAnd(f1, f2);
        assertEquals(f3, f2);
    }

    @Test
    public void testFormulaFactory4() {
        Formula f1 = FormulaFactory.mkLit("p1", false);
        Formula f2 = FormulaFactory.mkLit("p2", false);
        Formula f3 = FormulaFactory.mkAnd(f1, f2);
        Formula f4 = FormulaFactory.mkAnd(f1, f3);
        assertEquals(f3, f4);
    }

    @Test
    public void testFormulaFactory5() {
        Formula f0 = BooleanConstant.get(false);
        Formula f1 = BooleanConstant.get(false);
        Formula f2 = BooleanConstant.get(false);
        Formula f3 = FormulaFactory.mkOr(f0, f1, f2);
        assertEquals(f3, BooleanConstant.get(false));
    }

    @Test
    public void testFormulaFactory6() {
        Formula f1 = FormulaFactory.mkLit("p1", false);
        Formula f2 = FormulaFactory.mkLit("p2", false);
        Formula f3 = FormulaFactory.mkF(f1);
        Formula f4 = FormulaFactory.mkU(f3, f2);

        Formula f5 = FormulaFactory.mkOr(f2, FormulaFactory.mkF(FormulaFactory.mkAnd(FormulaFactory.mkX(f2), f3)));
        assertNotEquals(f4, f5);
    }

    @Test
    public void testFormulaFactory7() {
        Formula f1 = FormulaFactory.mkLit("p1", false);
        Formula f2 = FormulaFactory.mkX(f1);
        Formula f3 = FormulaFactory.mkF(f2);
        assertNotEquals(f3.toString(), "XFp1");
    }

    @Test
    public void unique1() {
        Formula f0 = FormulaFactory.mkLit("p0", false);
        Formula f1 = FormulaFactory.mkLit("p1", false);
        Formula f2 = FormulaFactory.mkLit("p2", false);
        Formula f3 = FormulaFactory.mkAnd(FormulaFactory.mkOr(f1, f2), f1);
        assertTrue(!f0.equals(f1));
        assertTrue(!f0.equals(f2));
        assertTrue(!f0.equals(f3));
        assertTrue(!f1.equals(f2));
        assertTrue(!f2.equals(f3));

    }

    @Test
    public void simplify1() {
        Formula f1 = FormulaFactory.mkLit("p1", false);
        Formula f2 = FormulaFactory.mkLit("p2", false);
        Formula f3 = FormulaFactory.mkOr(f1, f2);
        assertTrue(f3 instanceof Disjunction);

    }

    @Test
    public void simplify2() {
        Formula f0 = FormulaFactory.mkLit("p0", false);
        Formula f1 = FormulaFactory.mkLit("p1", false);
        Formula f2 = FormulaFactory.mkLit("p2", false);
        Formula f3 = FormulaFactory.mkNot(FormulaFactory.mkAnd(FormulaFactory.mkOr(f1, f2), f0));

        Formula f4 = FormulaFactory.mkLit("p0", true);
        Formula f5 = FormulaFactory.mkLit("p1", true);
        Formula f6 = FormulaFactory.mkLit("p2", true);
        Formula f7 = FormulaFactory.mkOr(f4, FormulaFactory.mkAnd(f5, f6));
        assertEquals(f3, f7);

    }

    @Test
    public void simplify3() {
        Formula f0 = FormulaFactory.mkLit("p0", false);
        Formula f1 = FormulaFactory.mkU(BooleanConstant.get(true), f0);
        Formula f2 = FormulaFactory.mkAnd(f0, f1);
        Formula f3 = FormulaFactory.mkNot(f2);
        assertNotEquals(f3, FormulaFactory.mkNot(f0));
    }

    @Test
    public void simplify4() {
        Formula f0 = FormulaFactory.mkLit("p0", false);
        Formula f1 = FormulaFactory.mkLit("p1", false);
        Formula f2 = FormulaFactory.mkU(f0, f1);
        Formula f3 = FormulaFactory.mkNot(f2);

        Formula f4 = FormulaFactory.mkLit("p0", true);
        Formula f5 = FormulaFactory.mkLit("p1", true);
        Formula f6 = FormulaFactory.mkU(f5, FormulaFactory.mkAnd(f4, f5));
        Formula f7 = FormulaFactory.mkOr(FormulaFactory.mkG(f5), f6);

        assertEquals(f3, f7);
    }

    @Test
    public void simplify5() {
        Formula f0 = FormulaFactory.mkLit("p0", false);
        Formula f1 = FormulaFactory.mkLit("p1", false);
        Formula f2 = FormulaFactory.mkG(FormulaFactory.mkU(f0, f1));
        Formula f3 = FormulaFactory.mkNot(f2);

        Formula f4 = FormulaFactory.mkLit("p0", true);
        Formula f5 = FormulaFactory.mkLit("p1", true);
        Formula f6 = FormulaFactory.mkU(f5, FormulaFactory.mkAnd(f4, f5));
        Formula f7 = FormulaFactory.mkF(FormulaFactory.mkOr(FormulaFactory.mkG(f5), f6));
        assertEquals(f3, f7);
    }

    @Test
    public void testMkOr() {
        Formula f0 = FormulaFactory.mkLit("p0", false);
        Formula f1 = FormulaFactory.mkLit("p1", false);
        Formula f2 = FormulaFactory.mkOr(f0, f1);
        assertTrue(f2.toString().equals("(p0|p1)") || f2.toString().equals("(p1|p0)"));
    }

    @Test
    public void testSetToConst1() {
        Formula f0 = FormulaFactory.mkLit("p0", false);
        Formula f1 = f0.accept(PseudoSubstitutionVisitor.getVisitor(), f0, true);
        assertEquals(f1, BooleanConstant.get(true));
    }

    @Test
    public void testSetToConst2() {
        Formula f1 = FormulaFactory.mkLit("p1", false);
        Formula f2 = FormulaFactory.mkLit("p2", false);
        Formula f3 = FormulaFactory.mkOr(f1, f2);
        Formula f4 = f3.accept(PseudoSubstitutionVisitor.getVisitor(), f2, false);
        assertEquals(f4, f1);
    }

    @Test
    public void testUnfold1() {
        Formula f0 = FormulaFactory.mkLit("p0", false);
        Formula f1 = FormulaFactory.mkLit("p1", false);
        Formula f2 = FormulaFactory.mkU(f0, f1);
        Formula f3 = f2.unfold(true);
        Formula f4 = FormulaFactory.mkOr(f1, FormulaFactory.mkAnd(f0, f2));
        assertEquals(f3, f4);
    }

    @Test
    public void testAssertValuation1() {
        Formula f1 = FormulaFactory.mkLit("p2", false);
        Formula f2 = FormulaFactory.mkG(f1);
        Formula f3 = FormulaFactory.mkAnd(f2, f1);
        assertEquals(Simplifier.simplify(f3.temporalStep(Collections.emptySet())), BooleanConstant.get(false));
    }

    @Test
    public void testAssertValuation2() {
        Formula f0 = FormulaFactory.mkLit("p0", true);
        Formula f1 = FormulaFactory.mkLit("p2", true);
        Formula f2 = FormulaFactory.mkAnd(f0, f1);
        Formula f3 = FormulaFactory.mkU(f1, f2);
        Formula f4 = FormulaFactory.mkG(f1);
        Formula f5 = FormulaFactory.mkAnd(f1, f4);
        Formula f6 = FormulaFactory.mkAnd(f1, f3);
        Formula f7 = FormulaFactory.mkOr(f2, f5, f6);

        assertEquals(Simplifier.simplify(f7.evaluate(new Literal("p0", false)).evaluate(new Literal("p2", false))),
                BooleanConstant.get(false));
    }

    @Test
    public void testAssertValuation3() {
        Formula f1 = FormulaFactory.mkLit("p2", true);
        Formula f4 = FormulaFactory.mkG(f1);
        Formula f5 = f4.unfold(true);
        Formula f6 = Simplifier.simplify(f5.temporalStep(Collections.singleton("p2")));
        assertEquals(f6, BooleanConstant.get(false));
    }

    @Test
    public void testAssertValuation4() {
        Formula f1 = FormulaFactory.mkLit("p0", false);
        Formula f2 = FormulaFactory.mkLit("p1", false);
        Formula f3 = FormulaFactory.mkLit("p2", false);
        Formula f4 = FormulaFactory.mkAnd(f1, f2, f3);
        Formula f6 = Simplifier.simplify(f4.evaluate((Literal) f1));
        assertEquals(f6, FormulaFactory.mkAnd(f2, f3));
    }

    @Test
    public void gSubformulas() {
        Formula f1 = FormulaFactory.mkLit("p1", false);
        Formula f2 = FormulaFactory.mkF(FormulaFactory.mkG(f1));

        assertEquals(Collections.singleton(new GOperator(f1)), f2.gSubformulas());
    }

    @Test
    public void testSimplifyForEntails1() {
        Formula f1 = FormulaFactory.mkLit("p0", false);

        Formula f4 = FormulaFactory.mkG(f1);
        Formula f5 = FormulaFactory.mkG(FormulaFactory.mkF(f1));
        Formula f6 = FormulaFactory.mkAnd(f4, f5);
        assertNotEquals(f6, FormulaFactory.mkG(FormulaFactory.mkAnd(f1, FormulaFactory.mkF(f1))));
    }

    @Test
    public void testSimplifyForEntails2() {
        Formula f1 = FormulaFactory.mkLit("p0", false);

        Formula f4 = FormulaFactory.mkX(f1);
        Formula f5 = FormulaFactory.mkX(FormulaFactory.mkF(f1));
        Formula f6 = FormulaFactory.mkOr(f4, f5);
        assertNotEquals(f6, FormulaFactory.mkX(FormulaFactory.mkOr(f1, FormulaFactory.mkF(f1))));
    }

    @Test
    public void testUnfold2() {
        Formula f1 = FormulaFactory.mkLit("p0", false);
        Formula f2 = FormulaFactory.mkLit("p1", false);
        Formula f3 = FormulaFactory.mkG(f1);
        Formula f4 = FormulaFactory.mkG(f2);
        Formula f5 = FormulaFactory.mkAnd(f3, f4);
        Formula f6 = FormulaFactory.mkAnd(f5, f1, f2);

        assertEquals(f6, Simplifier.simplify(f5.unfold(true)));
    }

    @Test
    public void testImplication1() {
        Formula f1 = FormulaFactory.mkLit("p0", false);
        Formula f2 = FormulaFactory.mkG(f1);
        Formula f3 = FormulaFactory.mkX(f1);
        Formula f4 = FormulaFactory.mkG(FormulaFactory.mkF(f3));
        Formula f5 = FormulaFactory.mkAnd(f4, f2);
        ImplicationVisitor v = ImplicationVisitor.getVisitor();
        assertEquals(f2.accept(v, f5), true);
    }

    @Test
    public void testSimplifyAggressively1() {
        Formula f1 = FormulaFactory.mkLit("p0", false);
        Formula f2 = FormulaFactory.mkG(FormulaFactory.mkF(f1));
        Formula f3 = FormulaFactory.mkX(f1);
        Formula f4 = FormulaFactory.mkG(FormulaFactory.mkF(f3));
        SimplifyAggressivelyVisitor v = SimplifyAggressivelyVisitor.getVisitor();
        assertEquals(f4.accept(v), f2);
    }

    @Test
    public void testSimplifyAggressively2() {
        Formula f1 = FormulaFactory.mkLit("p0", false);
        Formula f2 = FormulaFactory.mkG(f1);
        Formula f3 = FormulaFactory.mkX(f1);
        Formula f4 = FormulaFactory.mkG(FormulaFactory.mkF(f3));
        Formula f5 = FormulaFactory.mkAnd(f4, f2);
        SimplifyAggressivelyVisitor v = SimplifyAggressivelyVisitor.getVisitor();
        assertEquals(f5.accept(v), f2);
    }

    @Test
    public void testSimplifyAggressively3() {
        Formula f1 = FormulaFactory.mkLit("p0", false);
        Formula f2 = FormulaFactory.mkF(BooleanConstant.get(true));
        Formula f3 = FormulaFactory.mkAnd(f1, f2);

        SimplifyAggressivelyVisitor v = SimplifyAggressivelyVisitor.getVisitor();
        assertEquals(f3.accept(v), f1);
    }

    @Test
    public void testSimplifyAggressively4() {
        Formula f1 = FormulaFactory.mkLit("p0", false);
        Formula f2 = FormulaFactory.mkU(f1, f1);

        SimplifyAggressivelyVisitor v = SimplifyAggressivelyVisitor.getVisitor();
        assertEquals(f2.accept(v), f1);
    }

    @Test
    public void test_simplify_boolean1() {
        Formula f1 = FormulaFactory.mkLit("p0", false);
        Formula f2 = BooleanConstant.get(true);
        Formula f3 = FormulaFactory.mkAnd(f1, f2);
        assertEquals(f3, f1);
    }

    @Test
    public void test_setConst() {
        Formula f1 = FormulaFactory.mkLit("p0", false);
        Formula f2 = FormulaFactory.mkLit("p1", false);
        Formula f3 = FormulaFactory.mkF(FormulaFactory.mkAnd(f1, f2));
        assertEquals(f3.accept(PseudoSubstitutionVisitor.getVisitor(), f1, true), f3);
    }

}
