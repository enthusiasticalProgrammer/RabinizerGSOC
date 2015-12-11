package rabinizer.automata;

import rabinizer.exec.Tuple;

import java.util.Map;
import java.util.Set;

public class ProductAccState extends Tuple<ProductState, Map<Integer, Set<Integer>>> {

    public ProductAccState(ProductState ps, Map<Integer, Set<Integer>> accSets) {
        super(ps, accSets);
    }

    @Override
    public String toString() {
        return getLeft() + " " + getRight();
    }

}
