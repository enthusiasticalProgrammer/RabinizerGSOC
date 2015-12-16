package rabinizer.automata;

import java.util.List;

/**
 * @author jkretinsky
 */
public class GRabinPairT<S extends IState<S>> extends GRabinPair<TranSet<S>> {

    public GRabinPairT(TranSet<S> l, List<TranSet<S>> r) {
        super(l, r);
    }

}
