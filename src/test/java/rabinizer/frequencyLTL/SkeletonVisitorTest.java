package rabinizer.frequencyLTL;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Sets;

import ltl.Formula;
import ltl.UnaryModalOperator;
import ltl.parser.Parser;
import rabinizer.frequencyLTL.SkeletonVisitor;

import org.junit.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class SkeletonVisitorTest {

    SkeletonVisitor visitor = new SkeletonVisitor();

    @Test
    public void testSimple() {
        BiMap<String, Integer> mapping = ImmutableBiMap.of("a", 0, "b", 1);
        Formula formula = Parser.formula("G a | X G b", mapping);
        Set<Set<UnaryModalOperator>> newHashSet = Sets.newHashSet(
                Collections.singleton((UnaryModalOperator) Parser.formula("G a", mapping)), Collections.singleton((UnaryModalOperator) Parser.formula("G b", mapping)));
        Set<Set<UnaryModalOperator>> skeleton = newHashSet;
        assertEquals(skeleton, formula.accept(visitor));
    }

    @Test
    public void testSimple2() {
        BiMap<String, Integer> mapping = ImmutableBiMap.of("a", 0, "b", 1);
        Formula formula = Parser.formula("G a & F G b");
        Set<Set<UnaryModalOperator>> skeleton = Collections
                .singleton(Sets.newHashSet((UnaryModalOperator) Parser.formula("G a", mapping), (UnaryModalOperator) Parser.formula("G b", mapping)));
        assertEquals(skeleton, formula.accept(visitor));
    }
}