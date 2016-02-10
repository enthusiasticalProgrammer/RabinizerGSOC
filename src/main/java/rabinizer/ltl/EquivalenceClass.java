package rabinizer.ltl;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * EquivalenceClass interface.
 * <p>
 * The general contract of this interface is: If two implementing objects were
 * created from different factories, implies and equivalent have to return
 * {@code false}.
 */
public interface EquivalenceClass {

    @NotNull Formula getRepresentative();

    boolean implies(@NotNull EquivalenceClass equivalenceClass);

    /**
     * Check if two classes are equivalent. Implementing classes are expected to
     * implement equivalent and equals, such that they agree on their return
     * values.
     *
     * @param equivalenceClass
     * @return
     */
    boolean equivalent(EquivalenceClass equivalenceClass);

    EquivalenceClass unfold(boolean unfoldG);

    EquivalenceClass temporalStep(Set<String> valuation);

    @NotNull EquivalenceClass and(@NotNull EquivalenceClass eq);

    @NotNull EquivalenceClass or(@NotNull EquivalenceClass eq);

    boolean isTrue();

    boolean isFalse();

    Set<Formula> getSupport();
}
