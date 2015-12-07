package rabinizer.ltl;

import rabinizer.automata.GSet;

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
        return FormulaFactory.mkAnd(operand.unfold(), (this));
    }

    @Override
    public Formula unfoldNoG() {
        return this;
    }

    @Override
    public FOperator not() {
        return new FOperator(operand.not());
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
            return BooleanConstant.get(false);
        } else {
            return this;
        }
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
