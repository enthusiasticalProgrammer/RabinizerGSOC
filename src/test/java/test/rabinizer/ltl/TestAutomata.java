package test.rabinizer.ltl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import rabinizer.automata.MasterFolded;
import rabinizer.ltl.bdd.BDDForVariables;
import rabinizer.ltl.Formula;
import rabinizer.ltl.FormulaFactory;

public class TestAutomata {

    @Test
    public void testMasterFoldedNew() {
        Formula f1 = FormulaFactory.mkLit("p1", BDDForVariables.bijectionIdAtom.id("p1"), false);
        Formula f2 = FormulaFactory.mkLit("p1", BDDForVariables.bijectionIdAtom.id("p1"), false);
        Formula f3 = FormulaFactory.mkOr(f1, f2);
        Formula f4 = FormulaFactory.mkG(f3);

        MasterFolded m = new MasterFolded(f4);
        assertEquals(f4, m.formula);
    }
}
