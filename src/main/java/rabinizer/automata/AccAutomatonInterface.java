package rabinizer.automata;

import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;

import java.io.OutputStream;
import java.io.PrintStream;

public interface AccAutomatonInterface {
    void toHOA(HOAConsumer hoa) throws HOAConsumerException;

    void toDotty(PrintStream printStream);

    void acc(PrintStream printStream);

    int size();

    int pairNumber();
}
