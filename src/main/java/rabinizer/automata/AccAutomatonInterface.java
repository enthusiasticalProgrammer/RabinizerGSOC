package rabinizer.automata;

import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;

/**
 * @author jkretinsky
 */
public interface AccAutomatonInterface {

    void toHOANew(HOAConsumer hoa) throws HOAConsumerException;

    String toDotty();

    String acc();

    int size();

    int pairNumber();

}
