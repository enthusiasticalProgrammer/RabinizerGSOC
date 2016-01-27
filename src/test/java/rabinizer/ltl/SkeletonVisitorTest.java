package rabinizer.ltl;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import rabinizer.Util;

import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class SkeletonVisitorTest {

    SkeletonVisitor visitor;

    @Before
    public void setUp() throws Exception {
        visitor = new SkeletonVisitor();
    }

    @Test
    public void testSimple() {
        Formula formula = Util.createFormula("G a | X G b");
        Set<Set<GOperator>> skeleton = Sets.newHashSet(Collections.singleton((GOperator) Util.createFormula("G a")), Collections.singleton((GOperator) Util.createFormula("G b")));
        assertEquals(skeleton, formula.accept(visitor));
    }

    @Test
    public void testSimple2() {
        Formula formula = Util.createFormula("G a & F G b");
        Set<Set<GOperator>> skeleton = Collections.singleton(Sets.newHashSet((GOperator) Util.createFormula("G a"), (GOperator) Util.createFormula("G b")));
        assertEquals(skeleton, formula.accept(visitor));
    }
}