package rabinizer.ltl;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.*;

public abstract class EquivalenceClassTest {
    private EquivalenceClassFactory factory;
    private Formula contradiction;
    private Formula tautology;
    private Formula literal;

    public abstract EquivalenceClassFactory setUpFactory(Set<Formula> domain);

    @Before
    public void setUp() {
        contradiction = new Conjunction(new GOperator(new Literal("a", false)), new FOperator(new Literal("a", true)));
        tautology = new Disjunction(new XOperator(new Literal("b", true)), new XOperator(new Literal("b", false)));
        literal = new Literal("c", false);

        factory = setUpFactory(new Conjunction(contradiction, tautology, literal).getPropositions());
    }

    @Test
    public void testGetRepresentative() throws Exception {
        assertEquals(Simplifier.simplify(contradiction, Simplifier.Strategy.PROPOSITIONAL), factory.createEquivalenceClass(contradiction).getRepresentative());
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
        assertTrue(c.equivalent(factory.createEquivalenceClass(new Conjunction(literal, new Literal("c", true)))));
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

    @Test
    public void testEmptyDomain() {
        EquivalenceClassFactory factory = setUpFactory(Collections.emptySet());
        assertNotEquals(factory, null);
    }
}
