package rabinizer.ltl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class SimplifyBooleanVisitor implements Visitor<Formula> {

    private static SimplifyBooleanVisitor instance = new SimplifyBooleanVisitor();

    private SimplifyBooleanVisitor() {
        super();
    }

    public static SimplifyBooleanVisitor getVisitor() {
        return instance;
    }

    @Override
    public Formula visit(FOperator f) {
        return f;
    }

    @Override
    public Formula visit(Conjunction c) {
        Set<Formula> set = c.getAllChildrenOfConjunction();
        ArrayList<Formula> helper = new ArrayList<>();
        Set<Formula> toRemove = new HashSet<Formula>();

        for (Formula form : set) {
            if (form instanceof BooleanConstant) {
                if (!((BooleanConstant) form).value) {
                    return FormulaFactory.mkConst(false);
                }
                toRemove.add(form);
            }
        }
        set.removeAll(toRemove);
        toRemove.clear();

        // put all Literals together (and check for trivial
        // tautologies/contradictions like a and a /a and !a
        for (Formula form : set) {
            if (form instanceof Literal) {
                helper.add(form);
                toRemove.add(form);

            }
        }
        set.removeAll(toRemove);
        toRemove.clear();
        for (int i = 0; i < helper.size(); i++) {

            for (int j = helper.size() - 1; j > i; j--) {
                if (((Literal) helper.get(i)).atom.equals(((Literal) helper.get(j)).atom)) {
                    if (((Literal) helper.get(i)).negated == (((Literal) helper.get(j)).negated)) {
                        helper.remove(j);
                    } else {
                        return FormulaFactory.mkConst(false);
                    }
                }
            }
        }
        set.addAll(helper);

        if (set.equals(c.children)) {
            return c;
        } else {
            return FormulaFactory.mkAnd(set);
        }
    }

    @Override
    public Formula visit(Disjunction d) {

        Set<Formula> set = d.getAllChildrenOfDisjunction();
        ArrayList<Formula> helper = new ArrayList<>();
        Set<Formula> toRemove = new HashSet<Formula>();

        for (Formula form : set) {
            if (form instanceof BooleanConstant) {
                if (((BooleanConstant) form).value) {
                    return FormulaFactory.mkConst(true);
                }
                toRemove.add(form);
            }
        }
        set.removeAll(toRemove);
        toRemove.clear();

        // put all Literals together (and check for trivial
        // tautologies/contradictions like a and a /a and !a

        for (Formula form : set) {
            if (form instanceof Literal) {
                helper.add(form);
                toRemove.add(form);

            }
        }
        set.removeAll(toRemove);
        toRemove.clear();

        for (int i = 0; i < helper.size(); i++) {

            for (int j = i + 1; j < helper.size(); j++) {
                if (((Literal) helper.get(i)).atom.equals(((Literal) helper.get(j)).atom)) {
                    if (((Literal) helper.get(i)).negated == (((Literal) helper.get(j)).negated)) {
                        helper.remove(j);
                    } else {
                        return FormulaFactory.mkConst(true);
                    }
                }
            }
        }
        set.addAll(helper);

        if (set.equals(d.children)) {
            return d;
        } else {
            return FormulaFactory.mkOr(set);
        }
    }

    @Override
    public Formula visit(BooleanConstant b) {
        return b;
    }

    @Override
    public Formula visit(Literal l) {
        return l;
    }

    @Override
    public Formula visit(GOperator g) {
        return g;
    }

    @Override
    public Formula visit(UOperator u) {
        return u;
    }

    @Override
    public Formula visit(XOperator x) {
        return x;
    }
}
