package rabinizer.automata;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * @author jkretinsky
 */
public class AccSR extends ArrayList<Set<ProductDegenAccState>> {

    private static final long serialVersionUID = 1L;

    AccSR(AccTR accTR, DSRA dsra) {
        super();
        for (int i = 0; i < 2 * accTR.size(); i++) {
            this.add(new HashSet<>());
        }
        for (ProductDegenAccState s : dsra.states) {
            for (Integer i : s.getRight()) {
                this.get(i).add(s);
            }
        }
    }

    @Override
    public String toString() {
        String result = "Rabin state-based acceptance condition";
        for (int i = 0; i < size() / 2; i++) {
            result += "\nPair " + (i + 1) + "\nFin:\n" + get(2 * i) + "\nInf:\n" + get(2 * i + 1);
        }
        return result;
    }

    String accSets(ProductDegenAccState s) {
        String result = "{";
        Set<Integer> accSets = s.getRight();
        for (int i = 0; i < 2 * size(); i++) {
            if (accSets.contains(i)) {
                result += i + " ";
            }
        }
        return result + "}";
    }

}
