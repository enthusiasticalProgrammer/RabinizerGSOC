package rabinizer.ltl.z3;

import com.microsoft.z3.BoolExpr;
import org.junit.Test;
import rabinizer.ltl.Formula;
import rabinizer.Util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

public class Z3LibraryWrapperClassTest {

    @Test
    public void checkSatAssignmentNotNull() {
        Formula f = Util.createFormula("true");
        Z3EquivalenceClassFactory lib = new Z3EquivalenceClassFactory(f.getPropositions());
        BoolExpr b = lib.createZ3(f);

        assertNotEquals(lib.getSatAssignment(b), null);

    }

    @Test
    public void checkGetPropositionsOutOfBoolReturnsOnlyLiterals() {
        Formula f = Util.createFormula("true");
        Z3EquivalenceClassFactory lib = new Z3EquivalenceClassFactory(f.getPropositions());
        BoolExpr b = lib.createZ3(f);
        assertFalse(lib.getPropositionsOutOfBoolExpr(b).contains(b));
    }
}
