package rabinizer.ltl;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * @author jkretinsky
 */
public abstract class ModalOperator extends Formula {

    public final Formula operand;

    protected ModalOperator(Formula operand) {
        this.operand = operand;
    }

    @Override
    public String toString() {
        return getOperator() + operand.toString();
    }

    @Override
    public boolean hasSubformula(Formula f) {
        return this.equals(f) || operand.hasSubformula(f);
    }

    @Override
    // to be overrridden by GOperator
    public Set<GOperator> gSubformulas() {
        return operand.gSubformulas();
    }

    @Override
    // to be overrridden by GOperator
    public Set<GOperator> topmostGs() {
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
        ModalOperator that = (ModalOperator) o;
        return Objects.equals(operand, that.operand);
    }

    @Override
    protected int hashCodeOnce() {
        return Objects.hash(operand);
    }

    @Override
    public Formula temporalStep(Set<String> valuation) {
        return this;
    }

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

    @Override
    public Formula evaluate(Literal literal) {
        return this;
    }

    public Formula getOperand() {
        return operand;
    }

    @Override
    public Formula evaluate(Set<GOperator> Gs) {
        return this;
    }

    @Override
    public Optional<Literal> getAnUnguardedLiteral() {
        return Optional.empty();
    }

    protected abstract char getOperator();

}