package rabinizer.ltl;

public final class FOperator extends FormulaUnary {

    public FOperator(Formula f) {
        super(f);
    }

    @Override
    public String operator() {
        return "F";
    }

    @Override
    public Formula unfold() {
        // U(F phi) = U(phi) \/ X F U(phi)
        return FormulaFactory.mkOr(operand.unfold(), (this));
    }

    @Override
    public Formula unfoldNoG() {
        // U(F phi) = U(phi) \/ X F U(phi)
        return FormulaFactory.mkOr(operand.unfoldNoG(), (this));
    }

    @Override
    public GOperator not() {
        return new GOperator(operand.not());
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
        return operand.isPureUniversal();
    }

    @Override
    public boolean isSuspendable() {
        return operand.isPureUniversal() || operand.isSuspendable();
    }

}
