package rabinizer.automata;

import rabinizer.exec.Tuple;

import java.util.Arrays;
import java.util.Set;

/**
 * @author jkretinsky
 */
public class ProductDegenAccState extends Tuple<ProductDegenState, Set<Integer>> {

    public ProductDegenAccState(ProductDegenState pds, Set<Integer> accSets) {
        super(pds, accSets);
    }

    @Override
    public String toString() {
        String result = getLeft().toString();
        int[] orderedSets = new int[getRight().size()];
        int i = 0;
        for (Integer set : getRight()) {
            orderedSets[i] = set;
            i++;
        }
        Arrays.sort(orderedSets);
        for (i = 0; i < orderedSets.length; i++) {
            int j = orderedSets[i];
            result += " " + (j % 2 == 1 ? "+" : "-") + (j / 2 + 1);
        }
        return result;
    }

}
