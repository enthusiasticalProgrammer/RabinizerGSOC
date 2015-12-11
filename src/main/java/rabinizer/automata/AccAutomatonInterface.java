package rabinizer.automata;

/**
 * @author jkretinsky
 */
public interface AccAutomatonInterface {

    String toHOA();

    String toDotty();

    String acc();

    int size();

    int pairNumber();

}
