package rabinizer.ltl;


import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import rabinizer.ltl.bdd.GSet;

import java.util.HashSet;
import java.util.Set;

public final class GOperator extends FormulaUnary {

    public GOperator(Formula f) {
        super(f);
    }

    @Override
    public String operator() {
        return "G";
    }

    @Override
    public Formula unfold() {
        // U(F phi) = U(phi) \/ X F U(phi)
        return FormulaFactory.mkAnd(operand.unfold(), /*new XOperator*/ (this));
    }

    @Override
    public Formula unfoldNoG() {
        return this;
    }

    @Override
    public FOperator not() {
        return new FOperator(operand.not());
    }

    //============== OVERRIDE ====================
    @Override
    public boolean containsG() {
        return true;
    }

    @Override
    public Set<Formula> gSubformulas() {
        Set<Formula> r = operand.gSubformulas();
        r.add(operand);
        return r;
    }

    @Override
    public Set<Formula> topmostGs() {
        Set<Formula> result = new HashSet<>();
        result.add(this.operand);
        return result;
    }

    @Override
    public Formula substituteGsToFalse(GSet gSet) {
        if (gSet.contains(operand)) {
            return FormulaFactory.mkConst(false);
        } else {
            return this;
        }
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
                return "G" + child;
        }
    }

    @Override
    public Formula rmAllConstants() {
        Formula child = operand.rmAllConstants();
        if (child instanceof BooleanConstant) {
            return child;
        }
        return FormulaFactory.mkG(child);
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }

    @Override
    public boolean acceptBinarybool(AttributeBinaryVisitor v, Formula f) {
        return v.visitG(this, f);
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
