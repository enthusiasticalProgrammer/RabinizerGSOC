/*
 * Copyright (C) 2016  (See AUTHORS)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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