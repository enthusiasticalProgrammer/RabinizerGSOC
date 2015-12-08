package rabinizer.ltl;

import java.util.Set;

public final class XOperator extends ModalOperator {

    public XOperator(Formula f) {
        super(f);
    }

    @Override
    public Formula unfold(boolean unfoldG) {
        return this;
    }

    @Override
    public Formula not() {
        return new XOperator(operand.not());
    }

    @Override
    public Formula temporalStep(Set<String> valuation) {
        return operand;
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
        return operand.isPureUniversal();
    }

    @Override
    public boolean isSuspendable() {
        return operand.isSuspendable();
    }

    @Override
    protected char getOperator() {
        return 'X';
    }
}
