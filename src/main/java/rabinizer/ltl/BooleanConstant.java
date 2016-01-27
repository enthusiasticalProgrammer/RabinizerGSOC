package rabinizer.ltl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class BooleanConstant extends FormulaNullary {

    public static final BooleanConstant TRUE = new BooleanConstant(true);
    public static final BooleanConstant FALSE = new BooleanConstant(false);

    public final boolean value;

    private BooleanConstant(boolean value) {
        this.value = value;
    }

    public static BooleanConstant get(boolean value) {
        return value ? TRUE : FALSE;
    }

    @Override
    public String toString() {
        return value ? "true" : "false";
    }

    @Override
    public BooleanConstant not() {
        return value ? FALSE : TRUE;
    }

    @Override
    public Formula evaluate(Literal literal) {
        return this;
    }

    @Override
    public Optional<Literal> getAnUnguardedLiteral() {
        return Optional.empty();
    }

    @Override
    public Formula temporalStep(Set<String> valuation) {
        return this;
    }

    @Override
    public Set<Formula> getPropositions() {
        return new HashSet<>();
    }

    @Override
    public Set<Formula> getTopMostPropositions() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getAtoms() {
        return new HashSet<>();
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
        return true;
    }

    @Override
    public boolean isPureUniversal() {
        return true;
    }

    @Override
    public boolean isSuspendable() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BooleanConstant that = (BooleanConstant) o;
        return value == that.value;
    }

    @Override
    protected int hashCodeOnce() {
        return Boolean.hashCode(value);
    }
}
