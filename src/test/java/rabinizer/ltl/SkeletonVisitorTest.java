package rabinizer.ltl;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import rabinizer.ltl.bdd.BDDEquivalenceClassFactory;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SkeletonVisitorTest {

    private final static int MAX_ITERATIONS = 5;

    SkeletonVisitor visitor;

    @Before
    public void setUp() throws Exception {
        visitor = new SkeletonVisitor();
    }

    @Test
    public void testSimple() {
        Formula formula = Util.createFormula("X G F G a");
        Formula skeleton = Util.createFormula("G F G a & G a");
        assertEquals(skeleton, Simplifier.simplify(formula.accept(visitor)));
    }

    @Test
    public void invariantCheck() {
        for (Formula formula : FormulaStorage.formulae) {
            EquivalenceClassFactory factory = new BDDEquivalenceClassFactory(formula.getPropositions());
            EquivalenceClass skeletonClazz = factory.createEquivalenceClass(formula.accept(visitor));

            Formula current = formula;

            for (int i = 0; i < MAX_ITERATIONS; i++) {
                for (Set<GOperator> Gs : Sets.powerSet(current.gSubformulas())) {
                    EquivalenceClass gClazz = factory.createEquivalenceClass(new Conjunction(Gs));
                    EquivalenceClass currentClazz = factory.createEquivalenceClass(current);

                    if (!gClazz.implies(skeletonClazz)) {
                        assertTrue(!gClazz.implies(currentClazz));
                    }
                }

                current = current.unfold(true).temporalStep(current.getAtoms());
            }
        }
    }
}