package rabinizer.ltl.equivalence;

import org.jetbrains.annotations.NotNull;
import rabinizer.ltl.BooleanConstant;
import rabinizer.ltl.Formula;

public interface EquivalenceClassFactory {

    @NotNull EquivalenceClass createEquivalenceClass(@NotNull Formula formula);

    default @NotNull EquivalenceClass getTrue() {
        return createEquivalenceClass(BooleanConstant.TRUE);
    }

    default @NotNull EquivalenceClass getFalse() {
        return createEquivalenceClass(BooleanConstant.FALSE);
    }
}
