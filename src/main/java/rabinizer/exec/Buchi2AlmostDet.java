package rabinizer.exec;

import jhoafparser.consumer.HOAConsumerPrint;
import jhoafparser.parser.HOAFParser;
import jhoafparser.parser.generated.ParseException;
import rabinizer.automata.buchi.AlmostDeterminization;
import rabinizer.automata.buchi.BuchiAutomaton;
import rabinizer.automata.buchi.BuchiAutomatonBuilder;
import rabinizer.automata.buchi.SemiDeterminization;

public class Buchi2AlmostDet {

    public static void main(String[] args) throws ParseException {
        BuchiAutomatonBuilder builder = new BuchiAutomatonBuilder();
        HOAFParser.parseHOA(System.in, builder);

        for (BuchiAutomaton buchiAutomaton : builder.getAutomata()) {
            AlmostDeterminization algorithm = new AlmostDeterminization(buchiAutomaton);
            BuchiAutomaton almostDetAutomaton = algorithm.apply();
            almostDetAutomaton.toHOA(new HOAConsumerPrint(System.out));
        }
    }
}
