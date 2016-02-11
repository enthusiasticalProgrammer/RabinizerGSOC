package rabinizer.ltl.equivalence;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import rabinizer.collections.valuationset.BDDValuationSetFactory;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class BDDValuationSetFactoryTest {
    private BDDValuationSetFactory factory;
    private Set<String> alphabet;

    @Before
    public void setUp() throws Exception {
        alphabet = ImmutableSet.of("a", "b");
        factory = new BDDValuationSetFactory(alphabet);
    }

    @Test
    public void testGetAlphabet() throws Exception {
        assertEquals(alphabet, factory.getAlphabet());
    }

    @Test
    public void testCreateEmptyValuationSet() throws Exception {
        assertEquals(Collections.emptySet(), factory.createEmptyValuationSet());
    }

    @Test
    public void testCreateUniverseValuationSet() throws Exception {
        assertEquals(Sets.powerSet(alphabet), factory.createUniverseValuationSet());
    }

    @Test
    public void testCreateAllValuationSets() throws Exception {
        assertEquals(Sets.powerSet(alphabet).stream().map(factory::createValuationSet).collect(Collectors.toSet()), factory.createAllValuationSets());
    }
}