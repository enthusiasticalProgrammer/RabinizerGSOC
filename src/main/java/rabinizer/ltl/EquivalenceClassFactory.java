package rabinizer.ltl;

import org.jetbrains.annotations.NotNull;

public interface EquivalenceClassFactory {

    @NotNull EquivalenceClass createEquivalenceClass(@NotNull Formula formula);

    default @NotNull EquivalenceClass getTrue() {
        return createEquivalenceClass(BooleanConstant.TRUE);
    }

    default @NotNull EquivalenceClass getFalse() {
        return createEquivalenceClass(BooleanConstant.FALSE);
    }
}
