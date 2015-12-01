package rabinizer.ltl;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;

import java.util.ArrayList;

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
        return FormulaFactory.mkOr(operand.unfold(), /*new XOperator*/ (this));
    }

    @Override
    public Formula unfoldNoG() {
        // U(F phi) = U(phi) \/ X F U(phi)
        return FormulaFactory.mkOr(operand.unfoldNoG(), /*new XOperator*/ (this));
    }

    @Override
    public GOperator not() {
        return new GOperator(operand.not());
    }

    public BoolExpr toExpr(Context ctx) {
        if (cachedLTL == null) {
            cachedLTL = ctx.mkBoolConst(toZ3String(true));
        }
        return cachedLTL;
    }

    @Override
    public String toZ3String(boolean is_atom) {
        String child = operand.toZ3String(true);
        switch (child) {
            case "true":
                return "true";
            case "false":
                return "false";
            default:
                return "F" + child;
        }
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
        return FormulaFactory.mkF(child);
    }

    @Override
    public Formula acceptFormula(FormulaVisitor v) {
        return v.visitF(this);
    }

    @Override
    public boolean acceptBool(AttributeVisitor v) {
        return v.visitF(this);
    }

    @Override
    public boolean acceptBinarybool(AttributeBinaryVisitor v, Formula f) {
        return v.visitF(this, f);
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
