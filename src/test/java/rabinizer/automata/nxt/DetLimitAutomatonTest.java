package rabinizer.automata.nxt;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import jhoafparser.consumer.*;
import org.junit.Test;
import rabinizer.Util;
import rabinizer.automata.Optimisation;
import rabinizer.ltl.Formula;
import rabinizer.ltl.FormulaStorage;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class DetLimitAutomatonTest {

    public static void testOutput(String ltl, Set<Optimisation> opts, int size, String expectedOutput) throws HOAConsumerException, IOException {
        BiMap<String, Integer> mapping = HashBiMap.create();
        DetLimitAutomaton automaton = DetLimitAutomatonFactory.createDetLimitAutomaton(Util.createFormula(ltl, mapping), mapping, opts);

        assertEquals(automaton.toString(), size, automaton.size());

        if (expectedOutput != null) {
            assertEquals(expectedOutput, automaton.toString(true));
        }
    }

    public static void testOutput(String ltl, int size, String expectedOutput) throws HOAConsumerException, IOException {
        testOutput(ltl, EnumSet.allOf(Optimisation.class), size, expectedOutput);
    }

    public static void testOutput(String ltl, Set<Optimisation> opts, int size) throws HOAConsumerException, IOException {
        testOutput(ltl, opts, size, null);
    }

    public static void testOutput(String ltl, int size) throws HOAConsumerException, IOException {
        testOutput(ltl, EnumSet.allOf(Optimisation.class), size, null);
    }

    @Test
    public void testToHOA() throws Exception {
        String ltl = "((F G (a U c)) & G X b) | (F G X (f U d)) & X X e";
        testOutput(ltl, 9);
    }

    @Test
    public void testToHOA2() throws Exception {
        String ltl = "(G F a) | (G ((b | X ! a) & ((! b | X a))))";
        testOutput(ltl, 10);
    }

    @Test
    public void testToHOA3() throws Exception {
        String ltl = "G F a";
        testOutput(ltl, 1);
    }

    @Test
    public void testToHOA4() throws Exception {
        String ltl = "G a & F G F b";
        testOutput(ltl, 1);
    }

    @Test
    public void testToHOA5() throws Exception {
        String ltl = "G a | F G F b";
        testOutput(ltl, 3);
    }

    @Test
    public void testToHOA342() throws Exception {
        String ltl = "(F p) U (G q)";
        testOutput(ltl, 4);
    }

    @Test
    public void testDelayedJump() throws Exception {
        String ltl = "X a";
        testOutput(ltl, 3);
        testOutput(ltl, EnumSet.noneOf(Optimisation.class), 6);

        String ltl2 = "X X G b";
        testOutput(ltl2, 3);
        testOutput(ltl2, EnumSet.noneOf(Optimisation.class), 4);
    }

    @Test
    public void testOrBreakUp() throws Exception {
        String ltl = "G a | G b";
        testOutput(ltl, 3);
        testOutput(ltl, EnumSet.noneOf(Optimisation.class), 6);
    }

    @Test
    public void testToHOA6() throws Exception {
        String ltl = "G a | X G b";
        testOutput(ltl, 4);
    }

    @Test
    public void testToHOA7() throws Exception {
        String ltl = "X F (a U G b)";
        testOutput(ltl, 2);
    }

    @Test
    public void testToHOA73() throws Exception {
        String ltl = "G ((X X (a)) | (X b)) | G c";
        testOutput(ltl, 8);
    }

    @Test
    public void testOptimisations() throws Exception {
        String ltl = "((G F d | F G c) & (G F b | F G a) & (G F k | F G h))";
        testOutput(ltl, 9);
    }

    @Test
    public void testOptimisations2() throws Exception {
        String ltl = "G F (b | a)";
        testOutput(ltl, 1);
    }

    @Test
    public void testOptimisations3() throws Exception {
        String ltl = "G((a & !X a) | (X (a U (a & !b & (X(a & b & (a U (a & !b & (X(a & b))))))))))";
        testOutput(ltl, 9);
    }

    @Test
    public void testTrivial() throws Exception {
        String ltl = "a | !a";
        testOutput(ltl, 1);

        String ltl2 = "a & !a";
        testOutput(ltl2, 1);
    }

    @Test
    public void testRejectingCycle() throws Exception {
        String ltl = "!(G(a|b|c))";
        testOutput(ltl, 2);

        String ltl2 = "(G(a|b|c))";
        testOutput(ltl2, 1);
    }

    @Test
    public void testJumps() throws Exception {
        String ltl = "(G a) | X X X X b";
        testOutput(ltl, 11);
    }

    @Test
    public void testSanityCheckFailed() throws Exception {
        String ltl = "(G((F(!(a))) & (F((b) & (X(!(c))))) & (G(F((a) U (d)))))) & (G(F((X(d)) U ((b) | (G(c))))))";
        testOutput(ltl, 5);
    }

    @Test
    public void testSanityCheckFailed2() throws Exception {
        String ltl = "!(((X a) | (F b)) U (a))";
        testOutput(ltl, 7);
    }

    @Test
    public void testGR1() throws Exception {
        String ltl = "((G F b1 & G F b2) | F G !a2 | F G !a1)";
        testOutput(ltl, 4);
    }

    @Test
    public void testFOo() throws Exception {
        String ltl = "(G F c | F G b | F G a)";
        testOutput(ltl, 4);
    }

    @Test
    public void testEx() throws Exception {
        String ltl2 = "X G (a | F b)";
        testOutput(ltl2, 3);
        testOutput(ltl2, EnumSet.noneOf(Optimisation.class), 9);
    }

    @Test
    public void testSingle() throws Exception {
        String ltl = "G ((F a) & (F b))";
        testOutput(ltl, 1);
    }

    @Test
    public void testSCCPatient() throws Exception {
        final String testSCCHOA = "HOA: v1\n" +
                "tool: \"Rabinizer\" \"infty\"\n" +
                "AP: 3 \"a\" \"b\" \"c\"\n" +
                "acc-name: generalized-Buchi 1\n" +
                "Acceptance: 1 Inf(0)\n" +
                "Start: 0\n" +
                "--BODY--\n" +
                "State: 1\n[1] 2\n" +
                "State: 0\n[t] 3\n" +
                "State: 3\n[0] 1\n" +
                "State: 2\n[2] 2 {0}\n" +
                "--END--\n";

        String ltl = "X (a & (X (b & X G c)))";
        testOutput(ltl, 4, testSCCHOA);
    }

    @Test
    public void testAcceptanceSetSize() throws Exception {
        String ltl = "G ((p1 & p2 & (X(((p1)) | (p2)))) | G(! p2))";
        testOutput(ltl, 6);

        String ltl2 = "G(F(p0 & (G(F(p1))) | (G(!(p1)))))";
        testOutput(ltl2, 4);

        String ltl3 = "F(G(F(((p0) & (G(F(p1))) & (((!(p0)) & (p2)) | (F(p0)))) | ((F(G(!(p1)))) & ((!(p0)) | (((p0) | (!(p2))) & (G(!(p0)))))))))";
        testOutput(ltl3, 6);
    }

    @Test
    public void testCasePrism() throws Exception {
        String ltl = "(G F p1) & (F G ((p1) U (p3)))";
        testOutput(ltl, 2);
    }


    // @Test
    public void testBenchmarks() throws Exception {
        for (Map.Entry<File, List<Formula>> entry : FormulaStorage.formulaSets.entrySet()) {
            System.out.println("Testing: " + entry.getKey());

            for (Formula formula : entry.getValue()) {
                DetLimitAutomaton a = DetLimitAutomatonFactory.createDetLimitAutomaton(formula);
                a.toHOA(new HOAIntermediateCheckValidity(new HOAConsumerNull()));
                System.out.println(formula + " - Size: " + a.size());
            }
        }
    }

}