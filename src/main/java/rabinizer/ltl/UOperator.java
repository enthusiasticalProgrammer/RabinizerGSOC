package rabinizer.ltl;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;

import java.util.Objects;
import java.util.Set;

/**
 * Represents a until formula.
 *
 * @author Andreas & Ruslan
 */
public final class UOperator extends Formula {

    final Formula left, right;

    public UOperator(Formula left, Formula right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString() {
        if (cachedString == null) {
            cachedString = "(" + left + operator() + right + ")";
        }
        return cachedString;
    }

    @Override
    public boolean containsG() {
        return left.containsG() || right.containsG();
    }

    @Override
    public boolean hasSubformula(Formula f) {
        return this.equals(f) || left.hasSubformula(f) || right.hasSubformula(f);
    }

    @Override
    public Set<Formula> gSubformulas() {
        Set<Formula> r = left.gSubformulas();
        r.addAll(right.gSubformulas());
        return r;
    }

    @Override
    public Set<Formula> topmostGs() {
        Set<Formula> result = left.topmostGs();
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

    public String operator() {
        return "U";
    }

    @Override
    public Formula unfold() {
        // unfold(a U b) = unfold(b) v (unfold(a) ^ X (a U b))
        return FormulaFactory.mkOr(right.unfold(),
                FormulaFactory.mkAnd(left.unfold(), /* new XOperator */ (this)));
    }

    @Override
    public Formula unfoldNoG() {
        // unfold(a U b) = unfold(b) v (unfold(a) ^ X (a U b))
        return FormulaFactory.mkOr(right.unfoldNoG(),
                FormulaFactory.mkAnd(left.unfoldNoG(), /* new XOperator */ (this)));
    }

    @Override
    public Formula not() {
        return FormulaFactory.mkOr(FormulaFactory.mkG(right.not()),
                FormulaFactory.mkU(right.not(), FormulaFactory.mkAnd(left.not(), right.not())));
    }

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
    public Formula rmAllConstants() {
        Formula l = left.rmAllConstants();
        Formula r = right.rmAllConstants();
        if (l instanceof BooleanConstant) {
            if (((BooleanConstant) l).value) {
                return FormulaFactory.mkF(r);
            } else {
                return r;
            }
        }

        if (r instanceof BooleanConstant) {
            return r;
        }
        return FormulaFactory.mkU(l, r);
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
