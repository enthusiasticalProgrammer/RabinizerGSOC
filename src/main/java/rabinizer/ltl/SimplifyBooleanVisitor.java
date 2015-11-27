package rabinizer.ltl;

import java.util.ArrayList;
import java.util.Collections;

public class SimplifyBooleanVisitor implements FormulaVisitor {

    private static SimplifyBooleanVisitor instance = new SimplifyBooleanVisitor();

    private SimplifyBooleanVisitor() {
        super();
    }

    public static SimplifyBooleanVisitor getVisitor() {
        return instance;
    }


    public Formula visitF(FOperator f) {
        return f;
    }

    public Formula visitC(Conjunction c) {
        //first of all, get all subformulae beyound Conjunction(e.g. for c and (a and b)
        //I want a,b, and c, because you can simplify it more

        ArrayList<Formula> list = c.getAllChildrenOfConjunction();
        ArrayList<Formula> helper = new ArrayList<>();

        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i) instanceof BooleanConstant) {
                if (!((BooleanConstant) list.get(i)).get_value()) {
                    return FormulaFactory.mkConst(false);
                }
                list.remove(i);
            }
        }

        // remove dublicates
        for (int i = 0; i < list.size(); i++) {
            for (int j = list.size() - 1; j > i; j--) {
                if (list.get(i).get_id() == list.get(j).get_id()) {
                    list.remove(j);
                }
            }
        }

        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i) instanceof BooleanConstant) {
                if (!((BooleanConstant) list.get(i)).get_value()) {
                    return FormulaFactory.mkConst(false);
                }
                list.remove(i);
            }
        }

        // put all Literals together (and check for trivial
        // tautologies/contradictions like a and a /a and !a
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i) instanceof Literal) {
                helper.add(list.get(i));
                list.remove(i);

            }
        }
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
        list.addAll(helper);

        if (list.isEmpty()) {
            return FormulaFactory.mkConst(true);
        } else if (list.size() == 1) {
            return list.get(0);
        } else {
            // compare list and children and only make a new con-
            // junction if both are different (circumventing a stackoverflow)
            if (list.size() != c.children.size()) {
                return FormulaFactory.mkAnd(list);
            }

            // Therefore list has to be ordered
            Collections.sort(list, (f1, f2) -> Long.compare(f1.get_id(), f2.get_id()));

            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).get_id() != c.children.get(i).get_id()) {
                    return FormulaFactory.mkAnd(list);
                }
            }

            return c;
        }
    }


    public Formula visitD(Disjunction d) {

        ArrayList<Formula> list = d.getAllChildrenOfDisjunction();
        ArrayList<Formula> helper = new ArrayList<>();

        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i) instanceof BooleanConstant) {
                if (((BooleanConstant) list.get(i)).get_value()) {
                    return FormulaFactory.mkConst(true);
                }
                list.remove(i);
            }
        }

        //remove dublicates
        for (int i = 0; i < list.size(); i++) {
            for (int j = list.size() - 1; j > i; j--) {
                if (list.get(i).get_id() == list.get(j).get_id()) {
                    list.remove(j);
                }
            }
        }

        // put all Literals together (and check for trivial
        // tautologies/contradictions like a and a /a and !a
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i) instanceof Literal) {
                helper.add(list.get(i));
                list.remove(i);

            }
        }
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
        list.addAll(helper);
        if (list.isEmpty()) {
            return FormulaFactory.mkConst(false);
        } else if (list.size() == 1) {
            return list.get(0);
        } else {
            // compare list and children and only make a new dis-
            // junction if both are different (circumventing a stackoverflow)
            if (list.size() != d.children.size()) {
                return FormulaFactory.mkOr(list);
            }

            // Therefore list has to be ordered
            Collections.sort(list, (f1, f2) -> Long.compare(f1.get_id(), f2.get_id()));

            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).get_id() != d.children.get(i).get_id()) {
                    return FormulaFactory.mkOr(list);
                }
            }

            return d;
        }
    }

    public Formula visitB(BooleanConstant b) {
        return b;
    }

    public Formula visitL(Literal l) {
        return l;
    }

    public Formula visitG(GOperator g) {
        return g;
    }

    public Formula visitU(UOperator u) {
        return u;
    }

    public Formula visitX(XOperator x) {
        return x;
    }
}
