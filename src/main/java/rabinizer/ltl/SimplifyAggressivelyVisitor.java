package rabinizer.ltl;

import java.util.ArrayList;

public class SimplifyAggressivelyVisitor implements FormulaVisitor {

    private static SimplifyAggressivelyVisitor instance = new SimplifyAggressivelyVisitor();

    private SimplifyAggressivelyVisitor() {
        super();
    }

    public static SimplifyAggressivelyVisitor getVisitor() {
        return instance;
    }

    public Formula visitB(BooleanConstant b) {
        return b;
    }

    public Formula visitC(Conjunction c) {
        // first of all, get all subformulae beyound Conjunction(e.g. for c and
        // (a and b)
        // I want a,b, and c, because you can simplify it more

        ArrayList<Formula> list = c.getAllChildrenOfConjunction();

        for (int i = list.size() - 1; i >= 0; i--) {
            list.set(i, list.get(i).acceptFormula(this));
            if (list.get(i) instanceof BooleanConstant) {
                if (!((BooleanConstant) list.get(i)).get_value()) {
                    return FormulaFactory.mkConst(false);
                }
                list.remove(i);
            }
        }

        // remove dublicates and ltl that are implied by other Formulas
        for (int i = list.size() - 1; i >= 0; i--) {
            for (int j = list.size() - 1; j > i; j--) {
                if (list.get(i).get_id() == list.get(j).get_id()) {
                    list.remove(j);
                } else {
                    ImplicationVisitor imp = ImplicationVisitor.getVisitor();
                    if (list.get(i).acceptBinarybool(imp, list.get(j))) {
                        list.remove(j);
                    } else if (list.get(j).acceptBinarybool(imp, list.get(i))) {
                        list.remove(i);
                        break;
                    } else {
                        list.set(i, list.get(i).setToConst(list.get(j).get_id(), true));
                        list.set(j, list.get(j).setToConst(list.get(i).get_id(), true));
                    }
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

        if (list.isEmpty()) {
            return FormulaFactory.mkConst(true);
        } else if (list.size() == 1) {
            return list.get(0);
        } else {
            return FormulaFactory.mkAnd(list);
        }
    }

    public Formula visitD(Disjunction d) {
        ArrayList<Formula> list = d.getAllChildrenOfDisjunction();

        for (int i = list.size() - 1; i >= 0; i--) {
            list.set(i, list.get(i).acceptFormula(this));
            if (list.get(i) instanceof BooleanConstant) {
                if (((BooleanConstant) list.get(i)).get_value()) {
                    return FormulaFactory.mkConst(true);
                }
                list.remove(i);
            }
        }

        // remove dublicates or Formulas that are implied by other ltl
        for (int i = list.size() - 1; i >= 0; i--) {
            for (int j = list.size() - 1; j > i; j--) {
                if (list.get(i).get_id() == list.get(j).get_id()) {
                    list.remove(j);
                } else {
                    ImplicationVisitor imp = ImplicationVisitor.getVisitor();
                    if (list.get(i).acceptBinarybool(imp, list.get(j))) {
                        list.remove(i);
                        break;
                    } else if (list.get(j).acceptBinarybool(imp, list.get(i))) {
                        list.remove(j);
                    } else {
                        list.set(i, list.get(i).setToConst(list.get(j).get_id(), false));
                        list.set(j, list.get(j).setToConst(list.get(i).get_id(), false));
                    }
                }
            }
        }

        if (list.isEmpty()) {
            return FormulaFactory.mkConst(false);
        } else if (list.size() == 1) {
            return list.get(0);
        } else {
            return FormulaFactory.mkOr(list);
        }
    }

    public Formula visitF(FOperator f) {
        Formula child = f.operand.acceptFormula(this);
        if (child instanceof BooleanConstant) {
            return child;
        } else if (child instanceof UOperator) {
            return FormulaFactory.mkF(((FormulaBinary) child).right).acceptFormula(this);
        } else if (child instanceof FOperator) {
            return child;
        } else if (child instanceof XOperator) {
            return FormulaFactory.mkX(FormulaFactory.mkF(((FormulaUnary) child).operand)).acceptFormula(this);
        } else if (child instanceof Disjunction) {
            ArrayList<Formula> new_children = new ArrayList<>();
            for (Formula grandchild : ((FormulaBinaryBoolean) child).children) {
                new_children.add(FormulaFactory.mkF(grandchild));
            }
            return FormulaFactory.mkOr(new_children).acceptFormula(this);
        } else {
            return FormulaFactory.mkF(child);
        }
    }

    public Formula visitG(GOperator g) {
        Formula child = g.operand.acceptFormula(this);
        if (child instanceof BooleanConstant) {
            return child;
        } else if (child instanceof GOperator) {
            return child;
        } else if (child instanceof XOperator) {
            return FormulaFactory.mkX(FormulaFactory.mkG(((FormulaUnary) child).operand)).acceptFormula(this);
        }
        return FormulaFactory.mkG(child);
    }

    public Formula visitL(Literal l) {
        return l;
    }

    public Formula visitU(UOperator u) {
        if (u.right.isSuspendable() || u.right.isPureEventual()) {
            return u.right.acceptFormula(this);
        } else {
            Formula l = u.left.acceptFormula(this);
            Formula r = u.right.acceptFormula(this);
            ImplicationVisitor imp = ImplicationVisitor.getVisitor();
            if (l.acceptBinarybool(imp, r)) {
                return r;
            } else if (r instanceof BooleanConstant) {
                return r;
            } else if (l instanceof BooleanConstant) {
                if (((BooleanConstant) l).get_value()) {
                    return FormulaFactory.mkF(r).acceptFormula(this);
                } else {
                    return r;
                }
            } else if (r instanceof FOperator) {
                return r;
            } else if (l instanceof FOperator) {
                return FormulaFactory.mkOr(r, FormulaFactory.mkF(FormulaFactory.mkAnd(FormulaFactory.mkX(r), l)))
                        .acceptFormula(this);
            } else if (l.acceptBinarybool(imp, r)) {
                return r;
            } else if (l instanceof GOperator) {
                return FormulaFactory.mkOr(FormulaFactory.mkAnd(l, FormulaFactory.mkF(r)), r).acceptFormula(this);
            } else if (l instanceof XOperator && r instanceof XOperator) {
                return FormulaFactory.mkX(FormulaFactory.mkU(((FormulaUnary) l).operand, ((FormulaUnary) r).operand));
            }
            return FormulaFactory.mkU(l, r);
        }
    }

    public Formula visitX(XOperator x) {
        Formula child = x.operand.acceptFormula(this);
        if (child.isSuspendable()) {
            return child;
        } else if (child.get_id() == x.operand.get_id()) {
            return x;
        } else if (child instanceof BooleanConstant) {
            return child;
        } else {
            return FormulaFactory.mkX(child);
        }
    }

}
