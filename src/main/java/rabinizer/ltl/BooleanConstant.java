package rabinizer.ltl;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import net.sf.javabdd.BDD;
import rabinizer.ltl.bdd.BDDForFormulae;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class BooleanConstant extends FormulaNullary {

    static public final BooleanConstant TRUE = new BooleanConstant(true);
    static public final BooleanConstant FALSE = new BooleanConstant(false);

    public final boolean value;

    private BooleanConstant(boolean value) {
        this.value = value;
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
    public String toString() {
        if (cachedString == null) {
            cachedString = (value ? "true" : "false");
        }

        return cachedString;
    }

    @Override
    public BooleanConstant not() {
        return value ? FALSE : TRUE;
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
    public Set<Formula> getPropositions() {
        return new HashSet<>();
    }

    @Override
    public Formula rmAllConstants() {
        return FormulaFactory.mkConst(value);
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BooleanConstant that = (BooleanConstant) o;
        return value == that.value;
    }

    @Override
    protected int hashCodeOnce() {
        return Objects.hash(value);
    }
}
