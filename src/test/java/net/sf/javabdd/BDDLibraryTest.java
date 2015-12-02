package net.sf.javabdd;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BDDLibraryTest {

    private BDDFactory factory;

    @Before
    public void setup() {
        factory = BDDFactory.init("java", 100, 100);
        factory.setVarNum(2);
    }

    @After
    public void tearDown() {
        factory.reset();
    }

    @Test
    public void testHashCode() {
        BDD b1 = factory.ithVar(0);
        BDD b2 = factory.ithVar(1);

        assertEquals(b1.or(b2).hashCode(), b2.or(b1).hashCode());
        assertEquals(b1.and(b1).hashCode(), b1.hashCode());
    }

    @Test
    public void testEquals() {
        BDD b1 = factory.ithVar(0);
        BDD b2 = factory.ithVar(1);

        assertEquals(b1.or(b2), b2.or(b1));
        assertEquals(b1.and(b1), b1);
    }

    @Test
    public void testIsOne() {
        BDD b1 = factory.ithVar(0);
        BDD nb1 = factory.nithVar(0);

        assertTrue(b1.or(nb1).isOne());
    }

    @Test
    public void testIsZero() {
        BDD b1 = factory.ithVar(0);
        BDD nb1 = factory.nithVar(0);

        assertTrue(b1.and(nb1).isZero());
    }
}
