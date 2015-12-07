package rabinizer.ltl;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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
        return (Literal) FormulaFactory.mkLit(this.atom, false);
    }

    @Override
    public String toString() {
        if (cachedString == null) {
            cachedString = (negated ? "!" : "") + atom;
        }
        return cachedString;
    }

    @Override
    public Formula evaluate(Set<String> valuation) {
        return BooleanConstant.get(valuation.contains(atom) ^ negated);
    }

    @Override
    public Formula evaluate(Literal literal) {
        if (!literal.atom.equals(this.atom)) {
            return this;
        } else {
            return BooleanConstant.get(literal.negated == this.negated);
        }
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
    public BoolExpr toExpr(Context ctx) {

        if (cachedLTL == null) {
            cachedLTL = ctx.mkBoolConst(atom);
            if (negated) {
                cachedLTL = ctx.mkNot(cachedLTL);
            }
        }
        return cachedLTL;
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
