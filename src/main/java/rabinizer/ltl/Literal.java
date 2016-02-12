package rabinizer.ltl;

import java.util.*;

public final class Literal extends FormulaNullary {

    final String atom;
    final boolean negated;

    public Literal(String atom, boolean negated) {
        this.atom = atom;
        this.negated = negated;
    }

    public boolean getNegated() {
        return negated;
    }

    public Literal positiveLiteral() {
        return new Literal(this.atom, false);
    }

    @Override
    public String toString() {
        return negated ? '!' + atom : atom;
    }

    @Override
    public Formula evaluate(Literal literal) {
        if (literal.atom.equals(this.atom)) {
            return BooleanConstant.get(literal.negated == this.negated);
        }

        return this;
    }

    @Override
    public Literal not() {
        return new Literal(atom, !negated);
    }

    @Override
    public Optional<Literal> getAnUnguardedLiteral() {
        return Optional.of(this);
    }

    @Override
    public Formula temporalStep(Set<String> valuation) {
        return BooleanConstant.get(valuation.contains(atom) ^ negated);
    }

    @Override
    public Set<Formula> getPropositions() {
        Set<Formula> propositions = new HashSet<>();
        propositions.add(this);
        return propositions;
    }

    @Override
    public Set<String> getAtoms() {
        Set<String> atoms = new HashSet<>();
        atoms.add(this.atom);
        return atoms;
    }

    public String getAtom() {
        return atom;
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }

    @Override
    public <A, B> A accept(BinaryVisitor<A, B> v, B f) {
        return v.visit(this, f);
    }

    @Override
    public <A, B, C> A accept(TripleVisitor<A, B, C> v, B f, C c) {
        return v.visit(this, f, c);
    }

    @Override
    public boolean isPureEventual() {
        return false;
    }

    @Override
    public boolean isPureUniversal() {
        return false;
    }

    @Override
    public boolean isSuspendable() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Literal literal = (Literal) o;
        return negated == literal.negated && Objects.equals(atom, literal.atom);
    }

    @Override
    protected int hashCodeOnce() {
        return Objects.hash(atom, negated);
    }
}
