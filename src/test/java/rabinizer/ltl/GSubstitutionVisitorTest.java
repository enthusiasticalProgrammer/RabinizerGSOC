package rabinizer.ltl;

import org.junit.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.*;

public class GSubstitutionVisitorTest {

    @Test
    public void testVisit() throws Exception {
        GOperator G1 = (GOperator) Util.createFormula("G(p2)");
        GOperator G2 = (GOperator) Util.createFormula("G(F(G(p2)))");
        Formula formula = Util.createFormula("(p1) U (X((G(F(G(p2)))) & (F(X(X(G(p2)))))))");

        Set<GOperator> set1 = Collections.singleton(G1);
        Set<GOperator> set2 = Collections.singleton(G2);

        GSubstitutionVisitor visitor1 = new GSubstitutionVisitor(g -> set1.contains(g) ? BooleanConstant.TRUE : BooleanConstant.FALSE);
        GSubstitutionVisitor visitor2 = new GSubstitutionVisitor(g -> set2.contains(g) ? BooleanConstant.TRUE : null);

        assertEquals(Collections.emptySet(), formula.accept(visitor1).gSubformulas());
        assertEquals(set1, formula.accept(visitor2).gSubformulas());
    }
}