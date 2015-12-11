package rabinizer.automata;

import rabinizer.exec.Tuple;

import java.util.Map;

/**
 * @author jkretinsky
 */
public class ProductDegenState extends Tuple<ProductState, Map<Integer, Integer>> {

    public ProductDegenState(ProductState ps, Map<Integer, Integer> awaitedIndices) {
        super(ps, awaitedIndices);
    }

    @Override
    public String toString() {
        return getLeft() + " " + getRight();
    }

}
