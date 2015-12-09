package rabinizer.ltl;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Represents a until formula.
 *
 * @author Andreas & Ruslan
 */
public final class UOperator extends Formula {

    final Formula left;
    final Formula right;

    public UOperator(Formula left, Formula right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString() {
        return '(' + left.toString() + 'U' + right.toString() + ')';
    }

    @Override
    public boolean hasSubformula(Formula f) {
        return this.equals(f) || left.hasSubformula(f) || right.hasSubformula(f);
    }

    @Override
    public Set<GOperator> gSubformulas() {
        Set<GOperator> r = left.gSubformulas();
        r.addAll(right.gSubformulas());
        return r;
    }

    @Override
    public Set<GOperator> topmostGs() {
        Set<GOperator> result = left.topmostGs();
        result.addAll(right.topmostGs());
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UOperator uOperator = (UOperator) o;
        return Objects.equals(left, uOperator.left) && Objects.equals(right, uOperator.right);
    }

    @Override
    protected int hashCodeOnce() {
        return Objects.hash(left, right);
    }

    @Override
    public Formula unfold(boolean unfoldG) {
        // unfold(a U b) = unfold(b) v (unfold(a) ^ X (a U b))
        return new Disjunction(right.unfold(unfoldG),
                new Conjunction(left.unfold(unfoldG), this));
    }

    @Override
    public Formula temporalStep(Set<String> valuation) {
        return this;
    }

    @Override
    public Formula not() {
        return new Disjunction(new GOperator(right.not()), new UOperator(right.not(), new Conjunction(left.not(), right.not())));
    }

    @Override
    public Formula evaluate(Literal literal) {
        return this;
    }

    @Override
    public Formula evaluate(Set<GOperator> Gs) {
        return this;
    }

    @Override
    public Optional<Literal> getAnUnguardedLiteral() {
        return Optional.empty();
    }

    @Deprecated
    @Override
    public BoolExpr toExpr(Context ctx) {
        if (cachedLTL == null) {
            cachedLTL = ctx.mkBoolConst(this.toString());
        }
        return cachedLTL;
    }

    @Override
    public Set<Formula> getPropositions() {
        Set<Formula> propositions = left.getPropositions();
        propositions.addAll(right.getPropositions());
        propositions.add(this);
        return propositions;
    }

    @Override
    public Set<String> getAtoms() {
        Set<String> atoms = left.getAtoms();
        atoms.addAll(right.getAtoms());
        return atoms;
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
        return right.isPureEventual();
    }

    @Override
    public boolean isPureUniversal() {
        return left.isPureUniversal() && right.isPureUniversal();
    }

    @Override
    public boolean isSuspendable() {
        return right.isSuspendable();
    }
}
