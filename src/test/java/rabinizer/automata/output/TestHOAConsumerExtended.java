package rabinizer.automata.output;

import org.junit.Test;

import jhoafparser.consumer.HOAConsumerException;
import jhoafparser.consumer.HOAIntermediateCheckValidity;
import rabinizer.automata.Automaton;

public abstract class TestHOAConsumerExtended {

    public static HOAIntermediateCheckValidity TestConsumer() {
        return new HOAIntermediateCheckValidity(null);
    }

    @Test
    public void testHOA() throws HOAConsumerException {
        Automaton testAutomaton = getAutomaton();
        System.out.println("automaton obtained");
        testAutomaton.toHOA(TestConsumer());

    }

    protected abstract Automaton getAutomaton();
}
