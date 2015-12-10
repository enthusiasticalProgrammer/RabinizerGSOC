package rabinizer.ltl;

import org.junit.Test;
import rabinizer.automata.DTGRARaw;
import rabinizer.automata.MasterFolded;
import rabinizer.ltl.bdd.BDDEquivalenceClassFactory;
import rabinizer.ltl.bdd.BDDValuationSetFactory;
import rabinizer.parser.LTLParser;
import rabinizer.parser.ParseException;

import java.io.StringReader;

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

    @Test
    public void testDTGRARawConstructor() {
        LTLParser parser = new LTLParser(new StringReader("(p1) U (X((G(F(G(p2)))) & (F(X(X(G(p2)))))))"));

        Formula formula = null;
        try {
            formula = parser.parse();
        } catch (ParseException e) {
            throw new RuntimeException("Exception when parsing: " + e.getLocalizedMessage());
        }

        EquivalenceClassFactory equivalenceClassFactory = new BDDEquivalenceClassFactory(formula.getPropositions());
        ValuationSetFactory<String> valuationSetFactory = new BDDValuationSetFactory(formula.getAtoms());

        DTGRARaw dtgra = new DTGRARaw(formula, true, false, false, false, false, false, equivalenceClassFactory, valuationSetFactory);
    }
}
