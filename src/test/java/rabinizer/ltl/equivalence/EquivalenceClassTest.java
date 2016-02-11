package rabinizer.ltl.equivalence;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import rabinizer.Util;
import rabinizer.ltl.*;
import rabinizer.ltl.simplifier.Simplifier;

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
        contradiction = BooleanConstant.FALSE;
        tautology = BooleanConstant.TRUE;
        literal = new Literal("c", false);

        factory = setUpFactory(new Conjunction(contradiction, tautology, literal).getPropositions());
    }

    @Test
    public void testGetRepresentative() throws Exception {
        Assert.assertEquals(Simplifier.simplify(contradiction, Simplifier.Strategy.PROPOSITIONAL), factory.createEquivalenceClass(contradiction).getRepresentative());
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
        assertTrue(c.equivalent(factory.createEquivalenceClass(Simplifier.simplify(new Conjunction(literal, new Literal("c", true)), Simplifier.Strategy.MODAL_EXT))));
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

    @Test
    public void testUnfoldUnfold() {
        for (Formula formula : FormulaStorage.formulae) {
            EquivalenceClassFactory factory = setUpFactory(formula.getPropositions());
            EquivalenceClass clazz = factory.createEquivalenceClass(formula).unfold(true);
            assertEquals(clazz, clazz.unfold(true));
        }
    }

    @Test
    public void testGetSupport() throws Exception {
        Formula f = Util.createFormula("(F p1) & (!p2 | F p1)");
        EquivalenceClassFactory factory = setUpFactory(f.getPropositions());
        EquivalenceClass clazz = factory.createEquivalenceClass(f);
        assertEquals(Collections.singleton(Util.createFormula("F p1")), clazz.getSupport());
    }
}
