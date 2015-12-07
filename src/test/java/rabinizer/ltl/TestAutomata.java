package rabinizer.ltl;

import org.junit.Test;
import rabinizer.automata.MasterFolded;
import rabinizer.ltl.bdd.BDDEquivalenceClassFactory;
import rabinizer.ltl.bdd.BDDValuationSetFactory;

import static org.junit.Assert.assertEquals;

public class TestAutomata {

    @Test
    public void testMasterFoldedNew() {
        Formula f1 = FormulaFactory.mkLit("p1", false);
        Formula f2 = FormulaFactory.mkLit("p1", false);
        Formula f3 = FormulaFactory.mkOr(f1, f2);
        Formula f4 = FormulaFactory.mkG(f3);

        EquivalenceClassFactory factory = new BDDEquivalenceClassFactory(f4.getPropositions());
        ValuationSetFactory<String> valuationSetFactory = new BDDValuationSetFactory(f4.getAtoms());

        MasterFolded m = new MasterFolded(f4, factory, valuationSetFactory);
        assertEquals(f4, m.getFormula());
    }
}
