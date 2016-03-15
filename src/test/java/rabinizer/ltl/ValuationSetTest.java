package rabinizer.ltl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import rabinizer.collections.valuationset.ValuationSet;
import rabinizer.collections.valuationset.ValuationSetFactory;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.junit.Assert.*;

public abstract class ValuationSetTest {

    private Set<String> alphabet;

    private ValuationSetFactory factory;

    private ValuationSet universe;
    private ValuationSet empty;
    private ValuationSet abcd;
    private ValuationSet containsA;

    public abstract ValuationSetFactory setUpFactory(Set<String> domain);

    @Before
    public void setUp() throws Exception {
        alphabet = ImmutableSet.of("a", "b", "c", "d");
        factory = setUpFactory(alphabet);

        empty = factory.createEmptyValuationSet();
        universe = factory.createUniverseValuationSet();
        abcd = factory.createValuationSet(ImmutableSet.of("a", "b", "c", "d"));

        containsA = factory.createValuationSet(ImmutableSet.of("a"), ImmutableSet.of("a"));
    }

    @Test
    public void testComplement() throws Exception {
        assertEquals(universe.complement(), empty);
        assertEquals(empty.complement(), universe);
        assertEquals(abcd.complement().complement(), abcd);
        assertNotEquals(abcd.complement(), containsA);
    }

    @Test
    public void testIsUniverse() throws Exception {
        assertTrue(universe.isUniverse());

        assertFalse(empty.isUniverse());
        assertFalse(abcd.isUniverse());
        assertFalse(containsA.isUniverse());
    }

    @Test
    public void testRestrictWith() throws Exception {
        ValuationSet universeCopy = universe.clone();
        universeCopy.restrictWith(new Literal("a", false));
        assertEquals(containsA, universeCopy);

        universeCopy = universe.clone();
        universeCopy.restrictWith(new Literal("a", true));
        assertNotEquals(containsA, universeCopy);
    }

    @Test
    public void testPickAny() throws Exception {
        assertTrue(alphabet.containsAll(universe.pickAny()));
        assertTrue(containsA.pickAny().contains("a"));
        assertEquals(abcd.pickAny(), ImmutableSet.of("a", "b", "c", "d"));
    }

    @Test(expected = NoSuchElementException.class)
    public void testPickAnyNoSuchElement() throws Exception {
        empty.pickAny();
    }

    @Test
    public void testPickAny2() {
        assertFalse(abcd.pickAny().isEmpty());
    }

    @Test
    public void testIterator() {
        for (Set<String> valuation : universe) {
            assertTrue(Sets.powerSet(alphabet).contains(valuation));
        }

        for (Set<String> valuation : containsA) {
            assertTrue(valuation.contains("a"));
        }

        for (Set<String> valuation : abcd) {
            assertTrue(valuation.containsAll(Arrays.asList("a", "b", "c", "d")));
        }

        for (Set<String> valuation : empty) {
            fail("empty should be empty...");
        }
    }
}