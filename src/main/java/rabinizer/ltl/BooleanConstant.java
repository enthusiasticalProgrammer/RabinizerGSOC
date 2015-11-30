package rabinizer.ltl;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import net.sf.javabdd.BDD;
import rabinizer.ltl.bdd.BDDForFormulae;

import java.util.ArrayList;

public class BooleanConstant extends FormulaNullary {
    private final boolean value;

    private final int cachedHash;

    BooleanConstant(boolean value, long id) {
        super(id);
        this.value = value;
        this.cachedHash = init_hash();
    }

    @Override
    public String operator() {
        return null;
    }

    @Override
    public BDD bdd() {
        if (cachedBdd == null) {
            cachedBdd = (this.value ? BDDForFormulae.bddFactory.one() : BDDForFormulae.bddFactory.zero());
            BDDForFormulae.representativeOfBdd(cachedBdd, this);
        }
        return cachedBdd;
    }

    @Override
    public int hashCode() {
        return cachedHash;
    }

    public boolean get_value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BooleanConstant)) {
            return false;
        } else {
            return ((BooleanConstant) o).value == value;
        }
    }

    @Override
    public String toReversePolishString() {
        return toString();
    }

    @Override
    public String toString() {
        if (cachedString == null) {
            cachedString = (value ? "true" : "false");
        }
        return cachedString;
    }

    @Override
    public Formula negationToNNF() {
        return FormulaFactory.mkConst(!value);
    }

    public BoolExpr toExpr(Context ctx) {
        if (cachedLTL == null) {
            cachedLTL = (value ? ctx.mkTrue() : ctx.mkFalse());
        }
        return cachedLTL;

    }

    @Override
    public String toZ3String(boolean is_atom) {
        return (value ? "true" : "false");
    }

    @Override
    public ArrayList<String> getAllPropositions() {
        return new ArrayList<>();
    }

    @Override
    public Formula rmAllConstants() {
        return FormulaFactory.mkConst(value);
    }

    @Override
    public Formula setToConst(long id, boolean constant) {
        return this;
    }

    private int init_hash() {
        return value ? 1 : 2;
    }

    @Override
    public Formula acceptFormula(FormulaVisitor v) {
        return v.visitB(this);
    }

    @Override
    public boolean acceptBool(AttributeVisitor v) {
        return v.visitB(this);
    }

    @Override
    public boolean acceptBinarybool(AttributeBinaryVisitor v, Formula f) {
        return v.visitB(this, f);
    }

    @Override
    public boolean isPureEventual() {
        return true;
    }

    @Override
    public boolean isPureUniversal() {
        return true;
    }

    @Override
    public boolean isSuspendable() {
        return true;
    }
}
