/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package rabinizer.ltl;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;

import java.util.Objects;
import java.util.Set;

/**
 * @author jkretinsky
 */
public abstract class FormulaUnary extends Formula {

    public final Formula operand;

    protected FormulaUnary(Formula operand) {
        this.operand = operand;
    }

    @Override
    public String toString() {
        if (cachedString == null) {
            cachedString = operator() + operand;
        }
        return cachedString;
    }

    @Override
    public boolean hasSubformula(Formula f) {
        return this.equals(f) || operand.hasSubformula(f);
    }

    @Override
    // to be overrridden by GOperator
    public boolean containsG() {
        return operand.containsG();
    }

    @Override
    // to be overrridden by GOperator
    public Set<Formula> gSubformulas() {
        return operand.gSubformulas();
    }

    @Override
    // to be overrridden by GOperator
    public Set<Formula> topmostGs() {
        return operand.topmostGs();
    }

    @Override
    public BoolExpr toExpr(Context ctx) {
        if (cachedLTL == null) {
            cachedLTL = ctx.mkBoolConst(toString());
        }

        return cachedLTL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        FormulaUnary that = (FormulaUnary) o;
        return Objects.equals(operand, that.operand);
    }

    @Override
    protected int hashCodeOnce() {
        return Objects.hash(operand);
    }

    public abstract String operator();

    @Override
    public Set<Formula> getPropositions() {
        Set<Formula> propositions = operand.getPropositions();
        propositions.add(this);
        return propositions;
    }

    @Override
    public Set<String> getAtoms() {
        return operand.getAtoms();
    }
}
