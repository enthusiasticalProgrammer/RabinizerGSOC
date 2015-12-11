package rabinizer.automata;

import java.util.List;

/**
 * @author jkretinsky
 */
public class GRabinPairT extends GRabinPair<TranSet<ProductState>> {

    public GRabinPairT(TranSet<ProductState> l, List<TranSet<ProductState>> r) {
        super(l, r);
    }

}
