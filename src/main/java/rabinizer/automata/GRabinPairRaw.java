package rabinizer.automata;

import rabinizer.exec.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author jkretinsky
 */
public class GRabinPairRaw extends Tuple<TranSet<ProductState>, Set<TranSet<ProductState>>> {

    public GRabinPairRaw(TranSet<ProductState> l, Set<TranSet<ProductState>> r) {
        super(l, r);
    }

    @Override
    public String toString() {
        String result = "Fin:\n" + (getLeft() == null ? "trivial" : getLeft()) + "\nInf: ";
        if (getRight() == null || getRight().isEmpty()) {
            result += "0\ntrivial";
        } else {
            result += getRight().size();
            for (TranSet<ProductState> inf : getRight()) {
                result += "\n" + inf;
            }
        }
        return result;
    }

    public GRabinPairT order() {
        List<TranSet<ProductState>> rightOrdered = new ArrayList<>(getRight().size());
        for (TranSet<ProductState> ts : getRight()) {
            rightOrdered.add(ts);
        }
        return new GRabinPairT(getLeft(), rightOrdered);
    }

}
