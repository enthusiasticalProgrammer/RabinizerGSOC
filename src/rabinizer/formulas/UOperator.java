package rabinizer.formulas;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import net.sf.javabdd.BDD;
import rabinizer.bdd.BDDForFormulae;
import rabinizer.z3.LTLExpr;

import java.util.ArrayList;

/**
 * Represents a until formula.
 *
 * @author Andreas & Ruslan
 */
public class UOperator extends FormulaBinary {


    private final int cachedHash;

    UOperator(Formula left, Formula right, long id) {
        super(left, right, id);
        this.cachedHash = init_hash();
    }

    @Override
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

    public Formula toNNF() {
        return FormulaFactory.mkU(left.toNNF(), right.toNNF());
    }

    public Formula negationToNNF() {
        return FormulaFactory.mkOr(FormulaFactory.mkG(right.negationToNNF()),
                FormulaFactory.mkU(right.negationToNNF(), FormulaFactory.mkAnd(
                        left.negationToNNF(), right.negationToNNF())));
    }

    public BoolExpr toExpr(Context ctx) {
        if (cachedLTL == null) {
            cachedLTL = ctx.mkBoolConst(toZ3String(true));
        }
        return cachedLTL;
    }

    @Override
    public int hashCode() {
        return cachedHash;
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
    public ArrayList<String> getAllPropositions() {
        ArrayList<String> a = new ArrayList<>();
        if (!toZ3String(true).equals("true") && !toZ3String(true).equals("true"))
            a.add(toZ3String(true));
        return a;
    }

    @Override
    public Formula rmAllConstants() {
        Formula l = left.rmAllConstants();
        Formula r = right.rmAllConstants();
        if (l instanceof BooleanConstant) {
            if (((BooleanConstant) l).get_value()) {
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


    @Override
    public Formula setToConst(long id, boolean constant) {
        if (id == unique_id) {
            return FormulaFactory.mkConst(constant);
        } else {
            return this;
        }

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

    private int init_hash() {
        return (((left.hashCode() % 33767) * (right.hashCode() % 33049)) + 2141) % 999983;
    }


    @Override
    public Formula acceptFormula(FormulaVisitor v) {
        return v.visitU(this);
    }


    @Override
    public boolean acceptBool(AttributeVisitor v) {
        return v.visitU(this);
    }


    @Override
    public boolean acceptBinarybool(AttributeBinaryVisitor v, Formula f) {
        return v.visitU(this, f);
    }


}
