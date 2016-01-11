package rabinizer.ltl;

import com.google.common.collect.ImmutableSet;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @author jkretinsky & Christopher Ziegler
 */
public abstract class PropositionalFormula extends Formula {

    protected final Set<Formula> children;

    protected PropositionalFormula(Collection<? extends Formula> children) {
        this.children = ImmutableSet.copyOf(children);
    }

    protected PropositionalFormula(Formula... children) {
        this.children = ImmutableSet.copyOf(children);
    }

    protected PropositionalFormula(Stream<? extends Formula> formulaStream) {
        this.children = ImmutableSet.copyOf(formulaStream.iterator());
    }

    @Override
    public Formula unfold(boolean unfoldG) {
        return create(children.stream().map(c -> c.unfold(unfoldG)));
    }

    @Override
    public Formula evaluate(Literal literal) {
        return create(children.stream().map(c -> c.evaluate(literal)));
    }

    @Override
    public Formula evaluate(Set<GOperator> Gs) {
        return create(children.stream().map(c -> c.evaluate(Gs)));
    }

    @Override
    public Optional<Literal> getAnUnguardedLiteral() {
        for (Formula child : children) {
            Optional<Literal> literal = child.getAnUnguardedLiteral();

            if (literal.isPresent()) {
                return literal;
            }
        }

        return Optional.empty();
    }

    @Override
    public Set<GOperator> topmostGs() {
        return collect(Formula::topmostGs);
    }

    @Override
    public boolean ignoresG(Formula f) {
        if (hasSubformula(f)) {
            return children.stream().allMatch(c -> c.ignoresG(f));
        } else {
            return true;
        }
    }

    @Override
    public Set<Formula> getPropositions() {
        Set<Formula> propositions = new HashSet<>();

        for (Formula child : children) {
            for (Formula proposition : child.getPropositions()) {
                if (!propositions.contains(proposition.not())) {
                    propositions.add(proposition);
                }
            }
        }

        return propositions;
    }

    @Override
    public Set<GOperator> gSubformulas() {
        return collect(Formula::gSubformulas);
    }

    @Override
    public boolean hasSubformula(Formula f) {
        return this.equals(f) || anyMatch(c -> c.hasSubformula(f));
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(3 * children.size());

        s.append('(');

        Iterator<Formula> iter = children.iterator();

        while (iter.hasNext()) {
            s.append(iter.next());

            if (iter.hasNext()) {
                s.append(getOperator());
            }
        }

        s.append(')');

        return s.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PropositionalFormula that = (PropositionalFormula) o;
        return Objects.equals(children, that.children);
    }

    public Set<Formula> getChildren() {
        return children;
    }

    @Override
    public Set<String> getAtoms() {
        return collect(Formula::getAtoms);
    }

    @Override
    public boolean isPureEventual() {
        return allMatch(Formula::isPureEventual);
    }

    @Override
    public boolean isPureUniversal() {
        return allMatch(Formula::isPureUniversal);
    }

    @Override
    public boolean isSuspendable() {
        return allMatch(Formula::isSuspendable);
    }

    @Override
    public Formula temporalStep(Set<String> valuation) {
        return create(children.stream().map(c -> c.temporalStep(valuation)));
    }

    protected abstract PropositionalFormula create(Stream<? extends Formula> formulaStream);

    @Override
    protected int hashCodeOnce() {
        return Objects.hash(children);
    }

    protected abstract char getOperator();

    private <E> Set<E> collect(Function<Formula, Collection<E>> f) {
        Set<E> set = new HashSet<>(children.size());
        children.forEach(c -> set.addAll(f.apply(c)));
        return set;
    }

    private boolean anyMatch(Predicate<Formula> p) {
        return children.stream().anyMatch(p);
    }

    private boolean allMatch(Predicate<Formula> p) {
        return children.stream().allMatch(p);
    }
}
