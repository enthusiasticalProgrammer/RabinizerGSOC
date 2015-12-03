package rabinizer.ltl;

/**
 * EquivalenceClass interface.
 *
 * The general contract of this interface is: If two implementing objects were
 * created from different factories, implies and equivalent have to return
 * {@code false}.
 */
public interface EquivalenceClass {
    public Formula getRepresentative();

    public boolean implies(EquivalenceClass equivalenceClass);

    /**
     * Check if two classes are equivalent. Implementing classes are expected to
     * implement equivalent and equals, such that they agree on their return
     * values.
     *
     * @param equivalenceClass
     * @return
     */
    public boolean equivalent(EquivalenceClass equivalenceClass);
}