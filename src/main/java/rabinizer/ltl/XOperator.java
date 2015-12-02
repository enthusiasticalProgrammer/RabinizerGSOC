package rabinizer.ltl;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;


public final class XOperator extends FormulaUnary {

    public XOperator(Formula f) {
        super(f);
    }

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

    //============== OVERRIDE ====================
    @Override
    public Formula removeX() {
        return operand;
    }

    public BoolExpr toExpr(Context ctx) {
        if (cachedLTL == null) {
            cachedLTL = ctx.mkBoolConst(toZ3String(true));
        }

        return cachedLTL;
    }

    @Override
    public String toZ3String(boolean is_atom) {
        return "X" + operand.toZ3String(true);
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
    public boolean acceptBinarybool(AttributeBinaryVisitor v, Formula f) {
        return v.visitX(this, f);
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
