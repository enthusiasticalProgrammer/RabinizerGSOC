package rabinizer.automata;

import rabinizer.exec.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author jkretinsky
 */
public class GRabinPair<BaseSet> extends Tuple<BaseSet, List<BaseSet>> {

    public GRabinPair(BaseSet l, List<BaseSet> r) {
        super(l, r);
    }

    public final List<BaseSet> order(Set<BaseSet> r) {
        List<BaseSet> result = new ArrayList<>(r.size());
        for (BaseSet ts : r) {
            result.add(ts);
        }
        return result;
    }

    @Override
    public String toString() {
        String result = "Fin:\n" + (getLeft() == null ? "trivial" : getLeft()) + "\nInf: ";
        if (getRight() == null || getRight().isEmpty()) {
            result += "0\ntrivial";
        } else {
            result += getRight().size();
            for (BaseSet inf : getRight()) {
                result += "\n" + inf;
            }
        }
        return result;
    }

}
