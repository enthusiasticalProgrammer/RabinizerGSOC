package rabinizer.automata;

import rabinizer.collections.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author jkretinsky
 */
public class GRabinPairRaw<S extends IState<S>> extends Tuple<TranSet<S>, Set<TranSet<S>>> {

    public GRabinPairRaw(TranSet<S> l, Set<TranSet<S>> r) {
        super(l, r);
    }

    @Override
    public String toString() {
        String result = "Fin:\n" + (left == null ? "trivial" : left) + "\nInf: ";
        if (right == null || right.isEmpty()) {
            result += "0\ntrivial";
        } else {
            result += right.size();
            for (TranSet<S> inf : right) {
                result += "\n" + inf;
            }
        }
        return result;
    }

    public GRabinPairT order() {
        List<TranSet<S>> rightOrdered = new ArrayList<>(right.size());
        for (TranSet<S> ts : right) {
            rightOrdered.add(ts);
        }
        return new GRabinPairT(left, rightOrdered);
    }

}
