package rabinizer.automata.buchi;

import jhoafparser.consumer.HOAConsumerNull;
import jhoafparser.consumer.HOAConsumerPrint;
import jhoafparser.parser.HOAFParser;
import jhoafparser.parser.generated.ParseException;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class SemiDeterminisationTest {

    private final String input = "HOA: v1\n" +
            "States: 2\n" +
            "Start: 0\n" +
            "acc-name: Buchi\n" +
            "Acceptance: 1 Inf(0)\n" +
            "AP: 1 \"a\"\n" +
            "--BODY--\n" +
            "State: 0 {0}\n" +
            " [0]   1 \n" +
            "State: 1 \n" +
            " [t]   0 \n" +
            " [!0]  1 \n" +
            "--END--";

    @Test
    public void testConstruction() throws ParseException {
        BuchiAutomatonBuilder builder = new BuchiAutomatonBuilder();
        HOAFParser.parseHOA(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), builder);

        BuchiAutomaton buchi = builder.getAutomata().get(0);
        assertNotNull(buchi);

        SemiDeterminization det = new SemiDeterminization(buchi);
        BuchiAutomaton semi = det.apply();
        semi.toHOA(new HOAConsumerNull());

        assertEquals(8, semi.states.size());
        assertEquals(1, semi.acceptingStates.size());
        assertTrue(semi.acceptingStates.stream().noneMatch(buchi.acceptingStates::contains));
    }
}