package rabinizer.automata;

import rabinizer.exec.Tuple;

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
        String result = "Fin:\n" + (getLeft() == null ? "trivial" : getLeft()) + "\nInf: ";
        if (getRight() == null || getRight().isEmpty()) {
            result += "0\ntrivial";
        } else {
            result += getRight().size();
            for (TranSet<S> inf : getRight()) {
                result += "\n" + inf;
            }
        }
        return result;
    }

    public GRabinPairT order() {
        List<TranSet<S>> rightOrdered = new ArrayList<>(getRight().size());
        for (TranSet<S> ts : getRight()) {
            rightOrdered.add(ts);
        }
        return new GRabinPairT(getLeft(), rightOrdered);
    }

}
