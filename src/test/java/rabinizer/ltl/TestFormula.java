package rabinizer.ltl;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class TestFormula {

    @Test
    public void testFormulaEquality() {
        Formula f1 = new Literal("p1", false);
        Formula f2 = new Literal("p1", false);
        assertEquals(f1, f2);
    }

    @Test
    public void testFormulaEquality2() {
        Formula f1 = new Literal("p1", false);
        Formula f2 = new Literal("p1", true);
        assertEquals(!f1.equals(f2), true);
    }

    @Test
    public void testFormulaEquality3() {
        Formula f1 = new Literal("p1", false);
        Formula f2 = new Literal("p2", false);
        assertEquals(!f1.equals(f2), true);
    }

    @Test
    public void testFormulaFactory1() {
        Formula f1 = new Literal("p1", false);
        Formula f2 = new Literal("p1", false);
        Formula f3 = Simplifier.simplify(new Disjunction(f1, f2), Simplifier.Strategy.PROPOSITIONAL);
        assertEquals(f3, f2);
    }

    @Test
    public void testFormulaFactory2() {
        Formula f1 = new Literal("p1", false);
        Formula f2 = new Literal("p2", false);
        Formula f3 = Simplifier.simplify(new Disjunction(f1, f2), Simplifier.Strategy.PROPOSITIONAL);
        Formula f4 = Simplifier.simplify(new Disjunction(f1, f3), Simplifier.Strategy.PROPOSITIONAL);
        assertEquals(f3, f4);
    }

    @Test
    public void testFormulaFactory3() {
        Formula f1 = new Literal("p1", false);
        Formula f2 = new Literal("p1", false);
        Formula f3 = Simplifier.simplify(new Conjunction(f1, f2), Simplifier.Strategy.PROPOSITIONAL);
        assertEquals(f3, f2);
    }

    @Test
    public void testFormulaFactory4() {
        Formula f1 = new Literal("p1", false);
        Formula f2 = new Literal("p2", false);
        Formula f3 = Simplifier.simplify(new Conjunction(f1, f2), Simplifier.Strategy.PROPOSITIONAL);
        Formula f4 = Simplifier.simplify(new Conjunction(f1, f3), Simplifier.Strategy.PROPOSITIONAL);
        assertEquals(f3, f4);
    }

    @Test
    public void testFormulaFactory5() {
        Formula f0 = BooleanConstant.get(false);
        Formula f1 = BooleanConstant.get(false);
        Formula f2 = BooleanConstant.get(false);
        Formula f3 = Simplifier.simplify(new Disjunction(f0, f1, f2), Simplifier.Strategy.PROPOSITIONAL);
        assertEquals(f3, BooleanConstant.get(false));
    }

    @Test
    public void testFormulaFactory6() {
        Formula f1 = new Literal("p1", false);
        Formula f2 = new Literal("p2", false);
        Formula f3 = new FOperator(f1);
        Formula f4 = new UOperator(f3, f2);

        Formula f5 = Simplifier.simplify(new Disjunction(f2, new FOperator(Simplifier.simplify(new Conjunction(new XOperator(f2), f3), Simplifier.Strategy.PROPOSITIONAL))), Simplifier.Strategy.PROPOSITIONAL);
        assertNotEquals(f4, f5);
    }

    @Test
    public void testFormulaFactory7() {
        Formula f1 = new Literal("p1", false);
        Formula f2 = new XOperator(f1);
        Formula f3 = new FOperator(f2);
        assertNotEquals(f3.toString(), "XFp1");
    }

    @Test
    public void unique1() {
        Formula f0 = new Literal("p0", false);
        Formula f1 = new Literal("p1", false);
        Formula f2 = new Literal("p2", false);
        Formula f3 = Simplifier.simplify(new Conjunction(Simplifier.simplify(new Disjunction(f1, f2), Simplifier.Strategy.PROPOSITIONAL), f1), Simplifier.Strategy.PROPOSITIONAL);
        assertTrue(!f0.equals(f1));
        assertTrue(!f0.equals(f2));
        assertTrue(!f0.equals(f3));
        assertTrue(!f1.equals(f2));
        assertTrue(!f2.equals(f3));

    }

    @Test
    public void simplify1() {
        Formula f1 = new Literal("p1", false);
        Formula f2 = new Literal("p2", false);
        Formula f3 = Simplifier.simplify(new Disjunction(f1, f2), Simplifier.Strategy.PROPOSITIONAL);
        assertTrue(f3 instanceof Disjunction);

    }

    @Test
    public void simplify2() {
        Formula f0 = new Literal("p0", false);
        Formula f1 = new Literal("p1", false);
        Formula f2 = new Literal("p2", false);
        Formula f3 = Simplifier.simplify(new Conjunction(Simplifier.simplify(new Disjunction(f1, f2), Simplifier.Strategy.PROPOSITIONAL), f0), Simplifier.Strategy.PROPOSITIONAL).not();

        Formula f4 = new Literal("p0", true);
        Formula f5 = new Literal("p1", true);
        Formula f6 = new Literal("p2", true);
        Formula f7 = Simplifier.simplify(new Disjunction(f4, Simplifier.simplify(new Conjunction(f5, f6), Simplifier.Strategy.PROPOSITIONAL)), Simplifier.Strategy.PROPOSITIONAL);
        assertEquals(f3, f7);

    }

    @Test
    public void simplify3() {
        Formula f0 = new Literal("p0", false);
        Formula f1 = new UOperator(BooleanConstant.get(true), f0);
        Formula f2 = Simplifier.simplify(new Conjunction(f0, f1), Simplifier.Strategy.PROPOSITIONAL);
        Formula f3 = f2.not();
        assertNotEquals(f3, f0.not());
    }

    @Test
    public void simplify4() {
        Formula f0 = new Literal("p0", false);
        Formula f1 = new Literal("p1", false);
        Formula f2 = new UOperator(f0, f1);
        Formula f3 = f2.not();

        Formula f4 = new Literal("p0", true);
        Formula f5 = new Literal("p1", true);
        Formula f6 = new UOperator(f5, Simplifier.simplify(new Conjunction(f4, f5), Simplifier.Strategy.PROPOSITIONAL));
        Formula f7 = Simplifier.simplify(new Disjunction(new GOperator(f5), f6), Simplifier.Strategy.PROPOSITIONAL);

        assertEquals(f3, f7);
    }

    @Test
    public void simplify5() {
        Formula f0 = new Literal("p0", false);
        Formula f1 = new Literal("p1", false);
        Formula f2 = new GOperator(new UOperator(f0, f1));
        Formula f3 = f2.not();

        Formula f4 = new Literal("p0", true);
        Formula f5 = new Literal("p1", true);
        Formula f6 = new UOperator(f5, Simplifier.simplify(new Conjunction(f4, f5), Simplifier.Strategy.PROPOSITIONAL));
        Formula f7 = new FOperator(Simplifier.simplify(new Disjunction(new GOperator(f5), f6), Simplifier.Strategy.PROPOSITIONAL));
        assertEquals(f3, f7);
    }

    @Test
    public void testMkOr() {
        Formula f0 = new Literal("p0", false);
        Formula f1 = new Literal("p1", false);
        Formula f2 = Simplifier.simplify(new Disjunction(f0, f1), Simplifier.Strategy.PROPOSITIONAL);
        assertTrue(f2.toString().equals("(p0|p1)") || f2.toString().equals("(p1|p0)"));
    }

    @Test
    public void testSetToConst1() {
        Formula f0 = new Literal("p0", false);
        Formula f1 = f0.accept(PseudoSubstitutionVisitor.getVisitor(), f0, true);
        assertEquals(f1, BooleanConstant.get(true));
    }

    @Test
    public void testSetToConst2() {
        Formula f1 = new Literal("p1", false);
        Formula f2 = new Literal("p2", false);
        Formula f3 = Simplifier.simplify(new Disjunction(f1, f2), Simplifier.Strategy.PROPOSITIONAL);
        Formula f4 = f3.accept(PseudoSubstitutionVisitor.getVisitor(), f2, false);
        assertEquals(f4, f1);
    }

    @Test
    public void testUnfold1() {
        Formula f0 = new Literal("p0", false);
        Formula f1 = new Literal("p1", false);
        Formula f2 = new UOperator(f0, f1);
        Formula f3 = f2.unfold(true);
        Formula f4 = Simplifier.simplify(new Disjunction(f1, Simplifier.simplify(new Conjunction(f0, f2), Simplifier.Strategy.PROPOSITIONAL)), Simplifier.Strategy.PROPOSITIONAL);
        assertEquals(f3, f4);
    }

    @Test
    public void testAssertValuation1() {
        Formula f1 = new Literal("p2", false);
        Formula f2 = new GOperator(f1);
        Formula f3 = Simplifier.simplify(new Conjunction(f2, f1), Simplifier.Strategy.PROPOSITIONAL);
        assertEquals(Simplifier.simplify(f3.temporalStep(Collections.emptySet())), BooleanConstant.get(false));
    }

    @Test
    public void testAssertValuation2() {
        Formula f0 = new Literal("p0", true);
        Formula f1 = new Literal("p2", true);
        Formula f2 = Simplifier.simplify(new Conjunction(f0, f1), Simplifier.Strategy.PROPOSITIONAL);
        Formula f3 = new UOperator(f1, f2);
        Formula f4 = new GOperator(f1);
        Formula f5 = Simplifier.simplify(new Conjunction(f1, f4), Simplifier.Strategy.PROPOSITIONAL);
        Formula f6 = Simplifier.simplify(new Conjunction(f1, f3), Simplifier.Strategy.PROPOSITIONAL);
        Formula f7 = Simplifier.simplify(new Disjunction(f2, f5, f6), Simplifier.Strategy.PROPOSITIONAL);

        assertEquals(Simplifier.simplify(f7.evaluate(new Literal("p0", false)).evaluate(new Literal("p2", false))),
                BooleanConstant.get(false));
    }

    @Test
    public void testAssertValuation3() {
        Formula f1 = new Literal("p2", true);
        Formula f4 = new GOperator(f1);
        Formula f5 = f4.unfold(true);
        Formula f6 = Simplifier.simplify(f5.temporalStep(Collections.singleton("p2")));
        assertEquals(f6, BooleanConstant.get(false));
    }

    @Test
    public void testAssertValuation4() {
        Formula f1 = new Literal("p0", false);
        Formula f2 = new Literal("p1", false);
        Formula f3 = new Literal("p2", false);
        Formula f4 = Simplifier.simplify(new Conjunction(f1, f2, f3), Simplifier.Strategy.PROPOSITIONAL);
        Formula f6 = Simplifier.simplify(f4.evaluate((Literal) f1));
        assertEquals(f6, Simplifier.simplify(new Conjunction(f2, f3), Simplifier.Strategy.PROPOSITIONAL));
    }

    @Test
    public void gSubformulas() {
        Formula f1 = new Literal("p1", false);
        Formula f2 = new FOperator(new GOperator(f1));

        assertEquals(Collections.singleton(new GOperator(f1)), f2.gSubformulas());
    }

    @Test
    public void testSimplifyForEntails1() {
        Formula f1 = new Literal("p0", false);

        Formula f4 = new GOperator(f1);
        Formula f5 = new GOperator(new FOperator(f1));
        Formula f6 = Simplifier.simplify(new Conjunction(f4, f5), Simplifier.Strategy.PROPOSITIONAL);
        assertNotEquals(f6, new GOperator(Simplifier.simplify(new Conjunction(f1, new FOperator(f1)), Simplifier.Strategy.PROPOSITIONAL)));
    }

    @Test
    public void testSimplifyForEntails2() {
        Formula f1 = new Literal("p0", false);

        Formula f4 = new XOperator(f1);
        Formula f5 = new XOperator(new FOperator(f1));
        Formula f6 = Simplifier.simplify(new Disjunction(f4, f5), Simplifier.Strategy.PROPOSITIONAL);
        assertNotEquals(f6, new XOperator(Simplifier.simplify(new Disjunction(f1, new FOperator(f1)), Simplifier.Strategy.PROPOSITIONAL)));
    }

    @Test
    public void testUnfold2() {
        Formula f1 = new Literal("p0", false);
        Formula f2 = new Literal("p1", false);
        Formula f3 = new GOperator(f1);
        Formula f4 = new GOperator(f2);
        Formula f5 = Simplifier.simplify(new Conjunction(f3, f4), Simplifier.Strategy.PROPOSITIONAL);
        Formula f6 = Simplifier.simplify(new Conjunction(f5, f1, f2), Simplifier.Strategy.PROPOSITIONAL);

        assertEquals(f6, Simplifier.simplify(f5.unfold(true)));
    }

    @Test
    public void testImplication1() {
        Formula f1 = new Literal("p0", false);
        Formula f2 = new GOperator(f1);
        Formula f3 = new XOperator(f1);
        Formula f4 = new GOperator(new FOperator(f3));
        Formula f5 = Simplifier.simplify(new Conjunction(f4, f2), Simplifier.Strategy.PROPOSITIONAL);
        ImplicationVisitor v = ImplicationVisitor.getVisitor();
        assertEquals(f2.accept(v, f5), true);
    }

    @Test
    public void testSimplifyAggressively1() {
        Formula f1 = new Literal("p0", false);
        Formula f2 = new GOperator(new FOperator(f1));
        Formula f3 = new XOperator(f1);
        Formula f4 = new GOperator(new FOperator(f3));
        SimplifyAggressivelyVisitor v = SimplifyAggressivelyVisitor.getVisitor();
        assertEquals(f4.accept(v), f2);
    }

    @Test
    public void testSimplifyAggressively2() {
        Formula f1 = new Literal("p0", false);
        Formula f2 = new GOperator(f1);
        Formula f3 = new XOperator(f1);
        Formula f4 = new GOperator(new FOperator(f3));
        Formula f5 = Simplifier.simplify(new Conjunction(f4, f2), Simplifier.Strategy.PROPOSITIONAL);
        SimplifyAggressivelyVisitor v = SimplifyAggressivelyVisitor.getVisitor();
        assertEquals(f5.accept(v), f2);
    }

    @Test
    public void testSimplifyAggressively3() {
        Formula f1 = new Literal("p0", false);
        Formula f2 = new FOperator(BooleanConstant.get(true));
        Formula f3 = Simplifier.simplify(new Conjunction(f1, f2), Simplifier.Strategy.PROPOSITIONAL);

        SimplifyAggressivelyVisitor v = SimplifyAggressivelyVisitor.getVisitor();
        assertEquals(f3.accept(v), f1);
    }

    @Test
    public void testSimplifyAggressively4() {
        Formula f1 = new Literal("p0", false);
        Formula f2 = new UOperator(f1, f1);

        SimplifyAggressivelyVisitor v = SimplifyAggressivelyVisitor.getVisitor();
        assertEquals(f2.accept(v), f1);
    }

    @Test
    public void test_simplify_boolean1() {
        Formula f1 = new Literal("p0", false);
        Formula f2 = BooleanConstant.get(true);
        Formula f3 = Simplifier.simplify(new Conjunction(f1, f2), Simplifier.Strategy.PROPOSITIONAL);
        assertEquals(f3, f1);
    }

    @Test
    public void test_setConst() {
        Formula f1 = new Literal("p0", false);
        Formula f2 = new Literal("p1", false);
        Formula f3 = new FOperator(Simplifier.simplify(new Conjunction(f1, f2), Simplifier.Strategy.PROPOSITIONAL));
        assertEquals(f3.accept(PseudoSubstitutionVisitor.getVisitor(), f1, true), f3);
    }

}
