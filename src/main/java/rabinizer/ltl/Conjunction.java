package rabinizer.ltl;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import net.sf.javabdd.BDD;
import rabinizer.ltl.bdd.BDDForFormulae;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * @author Andreas Gaiser & Ruslan Ledesma-Garza & Christopher Ziegler
 */
public class Conjunction extends FormulaBinaryBoolean {


    private final int cachedHash;

    Conjunction(List<Formula> af, long id) {
        super(af, id);
        this.cachedHash = init_hash();
    }


    @Override
    public Formula ThisTypeBoolean(ArrayList<Formula> af) {
        return FormulaFactory.mkAnd(af);
    }

    @Override
    public String operator() {
        return "&";
    }


    @Override
    public BDD bdd() {
        if (cachedBdd == null) {
            cachedBdd = BDDForFormulae.bddFactory.one();
            for (Formula child : children) {
                cachedBdd = cachedBdd.and(child.bdd());
            }
            BDDForFormulae.representativeOfBdd(cachedBdd, this);
        }
        return cachedBdd;
    }


    @Override
    public int hashCode() {
        return cachedHash;
    }


    @Override
    public Formula removeConstants() {
        ArrayList<Formula> new_children = new ArrayList<>();
        for (Formula child : children) {
            Formula new_child = child.removeConstants();
            if (new_child instanceof BooleanConstant) {
                if (!((BooleanConstant) new_child).get_value()) {
                    return FormulaFactory.mkConst(false);
                }
            } else {
                new_children.add(new_child);
            }
        }
        if (new_children.size() == 1) {
            return new_children.get(0);
        } else {
            return FormulaFactory.mkAnd(new_children);
        }

    }

    @Override
    public boolean ignoresG(Formula f) {
        boolean isTransientwrt = !hasSubformula(f);
        boolean result = true;
        for (int i = 0; i < children.size(); i++) {
            for (int j = i + 1; j < children.size(); j++) {
                result = result && (children.get(i).isTransientwrt(children.get(j)) || children.get(j).isTransientwrt(children.get(i)));
            }
        }
        result = result || isTransientwrt;
        if (result) {
            return true;
        } else {    // don't know yet
            isTransientwrt = true;
            for (Formula child : children)
                isTransientwrt = isTransientwrt && child.ignoresG(f);
            return isTransientwrt;
        }

    }

    @Override
    public Formula toNNF() {
        ArrayList<Formula> nnf = new ArrayList<>();
        for (Formula child : children) {
            nnf.add(child.toNNF());
        }
        return FormulaFactory.mkAnd(nnf);
    }

    @Override
    public Formula negationToNNF() {
        ArrayList<Formula> negnnf = new ArrayList<>();
        for (Formula child : children) {
            negnnf.add(child.negationToNNF());
        }
        return FormulaFactory.mkOr(negnnf);
    }

    public BoolExpr toExpr(Context ctx) {
        if (cachedLTL == null) {
            ArrayList<BoolExpr> exprs = new ArrayList<>();
            for (Formula child : children) {
                exprs.add(child.toExpr(ctx));
            }
            BoolExpr[] helper = new BoolExpr[exprs.size()];
            exprs.toArray(helper);
            cachedLTL = ctx.mkAnd(helper);
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
                if (prop.equals("false")) {
                    return "false";
                } else if (!prop.equals("true")) {
                    result = result + (result.isEmpty() ? prop : " &" + prop);
                }
            }
            if (result == "") {
                return "true";
            } else {
                return result;
            }
        } else {
            result = "(and ";
            for (String prop : al) {
                if (prop.equals("false")) {
                    return "false";
                } else if (!prop.equals("true")) {
                    result = result + prop + " ";
                }
            }
            if (result.equals("(and ")) {
                return "true";
            } else {
                return result + " )";
            }
        }

    }


    @Override
    public Formula rmAllConstants() {
        ArrayList<Formula> new_children = new ArrayList<>();
        Formula fm;
        for (Formula child : children) {
            fm = child.rmAllConstants();
            if (fm instanceof BooleanConstant) {
                if (!((BooleanConstant) fm).get_value()) {
                    return FormulaFactory.mkConst(false);
                }
            } else {
                new_children.add(fm);
            }
        }
        if (new_children.isEmpty()) {
            return FormulaFactory.mkConst(true);
        } else if (new_children.size() == 1) {
            return new_children.get(0);
        } else {
            return FormulaFactory.mkAnd(new_children);
        }
    }


    //helps the SimplifyBooleanVisitor
    protected ArrayList<Formula> getAllChildrenOfConjunction() {
        ArrayList<Formula> al = new ArrayList<>();
        for (Formula child : children) {
            if (child instanceof Conjunction) {
                al.addAll(((Conjunction) child).getAllChildrenOfConjunction());
            } else {
                al.add(child);
            }
        }

        //sort them according to unique_id:
        Collections.sort(al, (f1, f2) -> Long.compare(f1.get_id(), f2.get_id()));

        return al;

    }

    private int init_hash() {
        int offset = 31607;
        int hash = 1;
        for (Formula child : children) {
            hash %= offset;
            hash = hash * (child.hashCode() % 31601);
        }

        return (hash + 1103) % 999983;
    }


    @Override
    public Formula acceptFormula(FormulaVisitor v) {
        return v.visitC(this);
    }


    @Override
    public boolean acceptBool(AttributeVisitor v) {
        return v.visitC(this);
    }


    @Override
    public boolean acceptBinarybool(AttributeBinaryVisitor v, Formula f) {
        return v.visitC(this, f);
    }

}
