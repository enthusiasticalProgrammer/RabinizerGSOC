package rabinizer.ltl.simplifier;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import rabinizer.Util;
import rabinizer.ltl.Formula;
import rabinizer.ltl.simplifier.Simplifier.Strategy;

public class SimplifierTest {

    private final Strategy strat = Simplifier.Strategy.AGGRESSIVELY;

    @Test
    public void testAggressiveSimplification1() {
        Formula f1 = Util.createFormula("G p0 & p0");
        Formula f2 = Util.createFormula("G p0");
        assertEquals(Simplifier.simplify(f1, strat), f2);
    }

    @Test
    public void testAggressiveSimplification2() {
        Formula f1 = Util.createFormula("G p0 | p0");
        Formula f2 = Util.createFormula("p0");
        assertEquals(Simplifier.simplify(f1, strat), f2);
    }

    @Test
    public void testAggressiveSimplification3() {
        Formula f1 = Util.createFormula("F p0 | p0");
        Formula f2 = Util.createFormula("F p0");
        assertEquals(Simplifier.simplify(f1, strat), f2);
    }

    @Test
    public void testAggressiveSimplification4() {
        Formula f1 = Util.createFormula("a & (b | (a & c))");
        Formula f2 = Util.createFormula("a & (b | c)");
        assertEquals(Simplifier.simplify(f1, strat), f2);
    }
}
