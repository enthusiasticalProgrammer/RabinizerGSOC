package rabinizer.exec;

import jhoafparser.consumer.HOAConsumerException;
import jhoafparser.consumer.HOAConsumerPrint;
import rabinizer.automata.nxt.DetLimitAutomaton;
import rabinizer.ltl.Formula;
import rabinizer.parser.LTLParser;

import java.io.IOException;
import java.io.StringReader;

public class LTL2DetLimit {

    public static void main(String[] args) throws IOException, rabinizer.parser.ParseException, HOAConsumerException {
        LTLParser parser = new LTLParser(new StringReader(args[0]));
        Formula formula = parser.parse();
        DetLimitAutomaton automaton = new DetLimitAutomaton(formula);
        automaton.toHOA(new HOAConsumerPrint(System.out));
    }
}
