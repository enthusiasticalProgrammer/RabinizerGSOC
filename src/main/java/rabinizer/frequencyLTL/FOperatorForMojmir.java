package rabinizer.frequencyLTL;

import java.util.HashSet;
import java.util.Set;

import ltl.BooleanConstant;
import ltl.FOperator;
import ltl.Formula;

public class FOperatorForMojmir extends FOperator {

    public FOperatorForMojmir(Formula f) {
        super(f);
    }

    @Override
    public Formula unfold(boolean unfoldG) {
        return unfoldG ? super.unfold(unfoldG) : this;
    }

    @Override
    public Set<Formula> topmostOperators() {
        Set<Formula> result = new HashSet<>();
        result.add(this);
        return result;
    }

    @Override
    public BooleanConstant evaluate(Set<Formula> Gs) {
        return BooleanConstant.get(Gs.contains(this));
    }
}
