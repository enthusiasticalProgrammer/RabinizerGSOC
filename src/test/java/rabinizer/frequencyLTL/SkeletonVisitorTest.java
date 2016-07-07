package rabinizer.frequencyLTL;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Sets;

import ltl.Formula;
import ltl.ModalOperator;
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
        Set<Set<ModalOperator>> newHashSet = Sets.newHashSet(Sets.newHashSet((ModalOperator) Parser.formula("G b", mapping), (ModalOperator) Parser.formula("G a", mapping)),
                Collections.singleton((ModalOperator) Parser.formula("G a", mapping)),
                Collections.singleton((ModalOperator) Parser.formula("G b", mapping)));
        Set<Set<ModalOperator>> skeleton = newHashSet;
        assertEquals(skeleton, formula.accept(visitor));
    }

    @Test
    public void testSimple2() {
        BiMap<String, Integer> mapping = ImmutableBiMap.of("a", 0, "b", 1);
        Formula formula = Parser.formula("G a & F G b");
        Set<Set<ModalOperator>> skeleton = Collections.singleton(Sets.newHashSet((ModalOperator) Parser.formula("G a", mapping), (ModalOperator) Parser.formula("G b", mapping)));
        assertEquals(skeleton, formula.accept(visitor));
    }
}