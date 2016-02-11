package rabinizer.exec;

import jhoafparser.consumer.HOAConsumerPrint;
import jhoafparser.parser.HOAFParser;
import jhoafparser.parser.generated.ParseException;
import rabinizer.automata.buchi.BuchiAutomaton;
import rabinizer.automata.buchi.BuchiAutomatonBuilder;
import rabinizer.automata.buchi.SemiDeterminization;

import java.io.IOException;

public class Buchi2DetLimit {

    public static void main(String... args) throws ParseException {
        BuchiAutomatonBuilder builder = new BuchiAutomatonBuilder();
        HOAFParser.parseHOA(System.in, builder);

        for (BuchiAutomaton buchiAutomaton : builder.getAutomata()) {
            SemiDeterminization algorithm = new SemiDeterminization(buchiAutomaton);
            BuchiAutomaton semiAutomaton = algorithm.apply();
            semiAutomaton.toHOA(new HOAConsumerPrint(System.out));
        }
    }
}
