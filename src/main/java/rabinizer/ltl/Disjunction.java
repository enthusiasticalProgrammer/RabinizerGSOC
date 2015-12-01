package rabinizer.ltl;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import net.sf.javabdd.BDD;
import rabinizer.ltl.bdd.BDDForFormulae;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Andreas Gaiser & Ruslan Ledesma-Garza & Christopher Ziegler
 */
public final class Disjunction extends FormulaBinaryBoolean {

    public Disjunction(Collection<Formula> disjuncts) {
        super(disjuncts);
    }

    public Disjunction(Formula... disjuncts) {
        super(Arrays.asList(disjuncts));
    }

    @Override
    public Formula ThisTypeBoolean(ArrayList<Formula> af) {
        return FormulaFactory.mkOr(af);
    }

    @Override
    public String operator() {
        return "|";
    }

    @Override
    public Formula removeConstants() {
        ArrayList<Formula> new_children = new ArrayList<>();
        for (Formula child : children) {
            Formula new_child = child.removeConstants();
            if (new_child instanceof BooleanConstant) {
                if (((BooleanConstant) new_child).value) {
                    return FormulaFactory.mkConst(true);
                }
            } else {
                new_children.add(new_child);
            }
        }
        if (new_children.isEmpty()) {
            return FormulaFactory.mkConst(false);
        }
        if (new_children.size() == 1) {
            return new_children.get(0);
        } else {
            return FormulaFactory.mkOr(new_children);
        }
    }

    @Override
    public boolean ignoresG(Formula f) {
        // return (!left.hasSubformula(f) || left.ignoresG(f))
        // && (!right.hasSubformula(f) || right.ignoresG(f));
        if (!hasSubformula(f)) {
            return true;
        } else {
            boolean ign = true;
            for (Formula child : children) {
                ign = ign && child.ignoresG(f);
            }
            return ign;
        }
    }

    @Override
    public Formula not() {
        return new Conjunction(children.stream().map(Formula::not).collect(Collectors.toSet()));
    }

    // ============================================================
    @Override
    public boolean isUnfoldOfF() {
        for (Formula child : children) {
            if (child instanceof XOperator) {
                if (((FormulaUnary) child).operand instanceof FOperator) {
                    return true;
                }
            }
        }
        return false;
    }

    public BoolExpr toExpr(Context ctx) {
        if (cachedLTL == null) {
            ArrayList<BoolExpr> exprs = new ArrayList<>();
            for (Formula child : children) {
                exprs.add(child.toExpr(ctx));
            }
            BoolExpr[] helper = new BoolExpr[exprs.size()];
            exprs.toArray(helper);
            cachedLTL = ctx.mkOr(helper);
        }
        return cachedLTL;

    }

    @Override
    public String toZ3String(boolean is_atom) {
        ArrayList<String> al = new ArrayList<>();
        for (Formula child : children) {
            al.add(child.toZ3String(is_atom));
        }

        String result = "";
        if (is_atom) {
            for (String prop : al) {
                if (prop.equals("true")) {
                    return "true";
                } else if (!prop.equals("false")) {
                    result = result + (result.isEmpty() ? prop : " &" + prop);
                }
            }
            if (result == "") {
                return "false";
            } else {
                return result;
            }
        } else {
            result = "(or ";
            for (String prop : al) {
                if (prop.equals("true")) {
                    return "true";
                } else if (!prop.equals("false")) {
                    result = result + prop + " ";
                }
            }
            if (result.equals("(or ")) {
                return "false";
            } else {
                return result + " )";
            }
        }
    }

    @Override
    public BDD bdd() {
        if (cachedBdd == null) {
            cachedBdd = BDDForFormulae.bddFactory.zero();
            for (Formula child : children) {
                cachedBdd = cachedBdd.or(child.bdd());
            }
            BDDForFormulae.representativeOfBdd(cachedBdd, this);
        }

        return cachedBdd;
    }

    @Override
    public Formula rmAllConstants() {
        ArrayList<Formula> new_children = new ArrayList<>();
        Formula fm;
        for (Formula child : children) {
            fm = child.rmAllConstants();
            if (fm instanceof BooleanConstant) {
                if (((BooleanConstant) fm).value) {
                    return FormulaFactory.mkConst(true);
                }
            } else {
                new_children.add(fm);
            }
        }
        if (new_children.isEmpty()) {
            return FormulaFactory.mkConst(false);
        } else if (new_children.size() == 1) {
            return new_children.get(0);
        } else {
            return FormulaFactory.mkOr(new_children);
        }
    }

    //helps the SimplifyBooleanVisitor
    protected Set<Formula> getAllChildrenOfDisjunction() {
        Set<Formula> al = new HashSet<>();

        for (Formula child : children) {
            if (child instanceof Disjunction) {
                al.addAll(((Disjunction) child).getAllChildrenOfDisjunction());
            } else {
                al.add(child);
            }
        }

        return al;
    }

    @Override
    public Formula acceptFormula(FormulaVisitor v) {
        return v.visitD(this);
    }

    @Override
    public boolean acceptBool(AttributeVisitor v) {
        return v.visitD(this);
    }

    @Override
    public boolean acceptBinarybool(AttributeBinaryVisitor v, Formula f) {
        return v.visitD(this, f);
    }

    @Override
    public boolean isPureEventual() {
        return children.stream().allMatch(c -> c.isPureEventual());
    }

    @Override
    public boolean isPureUniversal() {
        return children.stream().allMatch(c -> c.isPureUniversal());
    }

    @Override
    public boolean isSuspendable() {
        return children.stream().allMatch(c -> c.isSuspendable());
    }
}
