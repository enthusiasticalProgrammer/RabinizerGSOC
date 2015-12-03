package rabinizer.ltl.bdd;

import org.junit.Before;
import org.junit.Test;
import rabinizer.ltl.*;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/* TODO: Generalize this test to the interface */
public class BDDEquivalenceClassTest {

    private BDDEquivalenceClassFactory factory;

    private Formula contradiction;
    private Formula tautology;
    private Formula literal;

    @Before
    public void setup() {
        contradiction = new Conjunction(new GOperator(new Literal("a", 1, false)), new FOperator(new Literal("a", 1, true)));
        tautology = new Disjunction(new XOperator(new Literal("b", 2, true)), new XOperator(new Literal("b", 2, false)));
        literal = new Literal("c", 3, false);

        factory = new BDDEquivalenceClassFactory(new Conjunction(contradiction, tautology, literal).getPropositions());
    }

    @Test
    public void testGetRepresentative() throws Exception {
        assertEquals(contradiction, factory.createEquivalenceClass(contradiction).getRepresentative());
    }

    @Test
    public void testImplies() throws Exception {
        EquivalenceClass c = factory.createEquivalenceClass(contradiction);
        EquivalenceClass t = factory.createEquivalenceClass(tautology);
        EquivalenceClass l = factory.createEquivalenceClass(literal);

        assertTrue(c.implies(c));

        assertTrue(c.implies(t));
        assertTrue(c.implies(l));

        assertTrue(l.implies(t));
        assertTrue(!l.implies(c));

        assertTrue(!t.implies(c));
        assertTrue(!t.implies(l));
    }

    @Test
    public void testEquivalent() throws Exception {
        EquivalenceClass c = factory.createEquivalenceClass(contradiction);

        assertTrue(c.equivalent(c));
        assertTrue(c.equivalent(factory.createEquivalenceClass(new Conjunction(literal, new Literal("c", 3, true)))));
    }

    @Test
    public void testEqualsAndHashCode() throws Exception {
        Collection<EquivalenceClass> classes = new ArrayList<>();

        classes.add(factory.createEquivalenceClass(contradiction));
        classes.add(factory.createEquivalenceClass(tautology));
        classes.add(factory.createEquivalenceClass(literal));
        classes.add(factory.createEquivalenceClass(new Disjunction(tautology, contradiction, literal)));
        classes.add(factory.createEquivalenceClass(new Conjunction(tautology, contradiction, literal)));

        for (EquivalenceClass lhs : classes) {
            for (EquivalenceClass rhs : classes) {
                assertEquals(lhs.equivalent(rhs), lhs.equals(rhs));

                if (lhs.equals(rhs)) {
                    assertEquals(lhs.hashCode(), rhs.hashCode());
                }
            }
        }
    }
}