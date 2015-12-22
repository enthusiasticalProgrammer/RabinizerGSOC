package rabinizer.ltl.z3;

import org.junit.Test;
import static org.junit.Assert.*;

import com.microsoft.z3.BoolExpr;

import rabinizer.ltl.Formula;
import rabinizer.ltl.Util;

public class Z3LibraryWrapperClassTest {

    @Test
    public void checkSatAssignmentNotNull() {
        Formula f = Util.createFormula("true");
        Z3LibraryWrapper<Formula> lib = new Z3LibraryWrapper<Formula>(f.getPropositions());
        BoolExpr b = lib.createZ3(f);

        assertNotEquals(lib.getSatAssignment(b), null);

    }

    @Test
    public void checkGetPropositionsOutOfBoolReturnsOnlyLiterals() {
        Formula f = Util.createFormula("true");
        Z3LibraryWrapper<Formula> lib = new Z3LibraryWrapper<Formula>(f.getPropositions());
        BoolExpr b = lib.createZ3(f);
        assertFalse(lib.getPropositionsOutOfBoolExpr(b).contains(b));
    }
}
