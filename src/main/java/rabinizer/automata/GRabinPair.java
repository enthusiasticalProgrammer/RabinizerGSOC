package rabinizer.automata;

import rabinizer.collections.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author jkretinsky
 */
public class GRabinPair<S> extends Tuple<S, List<S>> {

    public GRabinPair(S l, List<S> r) {
        super(l, r);
    }

    @Override
    public String toString() {
        String result = "Fin:\n" + (left == null ? "trivial" : left) + "\nInf: ";
        if (right == null || right.isEmpty()) {
            result += "0\ntrivial";
        } else {
            result += right.size();
            for (S inf : right) {
                result += "\n" + inf;
            }
        }
        return result;
    }

}
