package rabinizer.ltl;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;

import java.util.ArrayList;


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
    public ArrayList<String> getAllPropositions() {
        ArrayList<String> a = new ArrayList<>();
        a.add(toZ3String(true));
        return a;
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
    public Formula acceptFormula(FormulaVisitor v) {
        return v.visitX(this);
    }

    @Override
    public boolean acceptBool(AttributeVisitor v) {
        return v.visitX(this);
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
