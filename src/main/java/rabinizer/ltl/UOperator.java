package rabinizer.ltl;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import net.sf.javabdd.BDD;
import rabinizer.ltl.bdd.BDDForFormulae;
import rabinizer.ltl.z3.LTLExpr;

import java.util.Objects;
import java.util.Set;

/**
 * Represents a until formula.
 *
 * @author Andreas & Ruslan
 */
public final class UOperator extends Formula {

    final Formula left, right;

    public UOperator(Formula left, Formula right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString() {
        if (cachedString == null) {
            cachedString = "(" + left + operator() + right + ")";
        }
        return cachedString;
    }

    @Override
    public boolean containsG() {
        return left.containsG() || right.containsG();
    }

    @Override
    public boolean hasSubformula(Formula f) {
        return this.equals(f) || left.hasSubformula(f) || right.hasSubformula(f);
    }

    @Override
    public Set<Formula> gSubformulas() {
        Set<Formula> r = left.gSubformulas();
        r.addAll(right.gSubformulas());
        return r;
    }

    @Override
    public Set<Formula> topmostGs() {
        Set<Formula> result = left.topmostGs();
        result.addAll(right.topmostGs());
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UOperator uOperator = (UOperator) o;
        return Objects.equals(left, uOperator.left) && Objects.equals(right, uOperator.right);
    }

    @Override
    protected int hashCodeOnce() {
        return Objects.hash(left, right);
    }

    public String operator() {
        return "U";
    }

    public BDD bdd() {
        if (cachedBdd == null) {
            Formula booleanAtom = FormulaFactory.mkU(
                    left.representative(),
                    right.representative()
            );
            int bddVar = BDDForFormulae.bijectionBooleanAtomBddVar.id(booleanAtom);
            if (BDDForFormulae.bddFactory.varNum() <= bddVar) {
                BDDForFormulae.bddFactory.extVarNum(bddVar);
            }
            cachedBdd = BDDForFormulae.bddFactory.ithVar(bddVar);
            BDDForFormulae.representativeOfBdd(cachedBdd, this);
        }
        return cachedBdd;
    }

    @Override
    public Formula unfold() {
        // unfold(a U b) = unfold(b) v (unfold(a) ^ X (a U b))
        return FormulaFactory.mkOr(right.unfold(), FormulaFactory.mkAnd(left.unfold(), /*new XOperator*/ (this)));
    }

    @Override
    public Formula unfoldNoG() {
        // unfold(a U b) = unfold(b) v (unfold(a) ^ X (a U b))
        return FormulaFactory.mkOr(right.unfoldNoG(), FormulaFactory.mkAnd(left.unfoldNoG(), /*new XOperator*/ (this)));
    }

    @Override
    public Formula not() {
        return FormulaFactory.mkOr(FormulaFactory.mkG(right.not()),
                FormulaFactory.mkU(right.not(), FormulaFactory.mkAnd(
                        left.not(), right.not())));
    }

    public BoolExpr toExpr(Context ctx) {
        if (cachedLTL == null) {
            cachedLTL = ctx.mkBoolConst(toZ3String(true));
        }
        return cachedLTL;
    }

    @Override
    public String toZ3String(boolean is_atom) {
        String l = left.toZ3String(true);
        String r = right.toZ3String(true);
        if (r.equals("true")) {
            return "true";
        } else if (l.equals("false")) {
            return r;
        } else if (r.equals("false")) {
            return "false";
        } else {
            return l + "U" + r;
        }

    }

    @Override
    public Set<Formula> getPropositions() {
        Set<Formula> propositions = left.getPropositions();
        propositions.addAll(right.getPropositions());
        propositions.add(this);
        return propositions;
    }

    @Override
    public Formula rmAllConstants() {
        Formula l = left.rmAllConstants();
        Formula r = right.rmAllConstants();
        if (l instanceof BooleanConstant) {
            if (((BooleanConstant) l).value) {
                return FormulaFactory.mkF(r);
            } else {
                return r;
            }
        }

        if (r instanceof BooleanConstant) {
            return r;
        }
        return FormulaFactory.mkU(l, r);
    }

    private BDD init_bdd() {
        BDD helper;
        Formula booleanAtom = FormulaFactory.mkU(
                left.representative(),
                right.representative());
        int bddVar = BDDForFormulae.bijectionBooleanAtomBddVar.id(booleanAtom);
        if (BDDForFormulae.bddFactory.varNum() <= bddVar) {
            BDDForFormulae.bddFactory.extVarNum(bddVar);
        }
        helper = BDDForFormulae.bddFactory.ithVar(bddVar);
        BDDForFormulae.representativeOfBdd(helper, this);
        return helper;
    }

    private BoolExpr init_ltl() {
        Context ctx = LTLExpr.getContext();
        return ctx.mkBoolConst(toZ3String(true));
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }

    @Override
    public boolean acceptBinarybool(AttributeBinaryVisitor v, Formula f) {
        return v.visitU(this, f);
    }

    @Override
    public boolean isPureEventual() {
        return right.isPureEventual();
    }

    @Override
    public boolean isPureUniversal() {
        return left.isPureUniversal() && right.isPureUniversal();
    }

    @Override
    public boolean isSuspendable() {
        return right.isSuspendable();
    }
}
