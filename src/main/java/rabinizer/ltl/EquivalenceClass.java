package rabinizer.ltl;

import java.util.Set;

/**
 * EquivalenceClass interface.
 * <p>
 * The general contract of this interface is: If two implementing objects were
 * created from different factories, implies and equivalent have to return
 * {@code false}.
 */
public interface EquivalenceClass {
    Formula getRepresentative();

    boolean implies(EquivalenceClass equivalenceClass);

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

    EquivalenceClass and(EquivalenceClass eq);

    EquivalenceClass or(EquivalenceClass eq);

    boolean isTrue();

    boolean isFalse();
}
