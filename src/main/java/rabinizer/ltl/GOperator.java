package rabinizer.ltl;

import com.google.common.collect.Sets;

import java.util.Set;

public final class GOperator extends ModalOperator {

    public GOperator(Formula f) {
        super(f);
    }

    @Override
    public char getOperator() {
        return 'G';
    }

    @Override
    public Formula unfold(boolean unfoldG) {
        if (unfoldG) {
            return new Conjunction(operand.unfold(true), this);
        }

        return this;
    }

    @Override
    public FOperator not() {
        return new FOperator(operand.not());
    }

    @Override
    public Set<GOperator> gSubformulas() {
        Set<GOperator> r = operand.gSubformulas();
        r.add(this);
        return r;
    }

    @Override
    public BooleanConstant evaluate(Set<GOperator> Gs) {
        return BooleanConstant.get(Gs.contains(this));
    }

    @Override
    public Set<GOperator> topmostGs() {
        return Sets.newHashSet(this);
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
        return operand.isPureEventual();
    }

    @Override
    public boolean isPureUniversal() {
        return true;
    }

    @Override
    public boolean isSuspendable() {
        return operand.isPureEventual() || operand.isSuspendable();
    }
}
