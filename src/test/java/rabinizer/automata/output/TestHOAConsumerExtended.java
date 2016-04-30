package rabinizer.automata.output;

import jhoafparser.consumer.HOAConsumerNull;
import org.junit.Test;

import jhoafparser.consumer.HOAConsumerException;
import jhoafparser.consumer.HOAIntermediateCheckValidity;
import rabinizer.automata.Automaton;

public abstract class TestHOAConsumerExtended {

    public static HOAIntermediateCheckValidity getTestConsumer() {
        return new HOAIntermediateCheckValidity(new HOAConsumerNull());
    }

    @Test
    public void testHOA() throws HOAConsumerException {
        Automaton testAutomaton = getAutomaton();
        System.out.println("automaton obtained");
        testAutomaton.toHOA(getTestConsumer());
    }

    protected abstract Automaton getAutomaton();
}
