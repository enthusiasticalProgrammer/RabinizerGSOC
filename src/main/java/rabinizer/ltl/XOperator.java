package rabinizer.ltl;

public final class XOperator extends FormulaUnary {

    public XOperator(Formula f) {
        super(f);
    }

    @Override
    public String operator() {
        return "X";
    }

    @Override
    public Formula unfold() {
        return this;
    }

    @Override
    public Formula unfoldNoG() {
        return this;
    }

    @Override
    public Formula not() {
        return new XOperator(operand.not());
    }

    // ============== OVERRIDE ====================
    @Override
    public Formula removeX() {
        return operand;
    }

    @Override
    public Formula rmAllConstants() {
        Formula child = operand.rmAllConstants();

        if (child instanceof BooleanConstant) {
            return child;
        }

        return FormulaFactory.mkX(child);
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
}
