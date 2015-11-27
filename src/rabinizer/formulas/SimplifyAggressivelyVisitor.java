package rabinizer.formulas;

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

        // remove dublicates and formulas that are implied by other Formulas
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
                    } else if (list.get(i).acceptBinarybool(imp, FormulaFactory.mkNot(list.get(j)).acceptFormula(this))) {
                        return FormulaFactory.mkConst(false);
                    } else if (list.get(j).acceptBinarybool(imp,
                            FormulaFactory.mkNot(list.get(i)).acceptFormula(this))) {
                        return FormulaFactory.mkConst(false);
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

        if (list.size() == 0) {
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

        // remove dublicates or Formulas that are implied by other formulas
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
                    } else if (FormulaFactory.mkNot((list.get(j)).acceptFormula(this)).acceptBinarybool(imp,
                            list.get(i))) {
                        return FormulaFactory.mkConst(true);
                    } else if (FormulaFactory.mkNot((list.get(i)).acceptFormula(this)).acceptBinarybool(imp,
                            list.get(j))) {
                        return FormulaFactory.mkConst(true);
                    } else {
                        list.set(i, list.get(i).setToConst(list.get(j).get_id(), false));
                        list.set(j, list.get(j).setToConst(list.get(i).get_id(), false));
                    }
                }
            }
        }

        if (list.size() == 0) {
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
            return FormulaFactory.mkF(((UOperator) child).right).acceptFormula(this);
        } else if (child instanceof FOperator) {
            return child;
        } else if (child instanceof XOperator) {
            return FormulaFactory.mkX(FormulaFactory.mkF(((XOperator) child).operand)).acceptFormula(this);
        } else if (child instanceof Disjunction) {
            ArrayList<Formula> new_children = new ArrayList<Formula>();
            for (Formula grandchild : ((Disjunction) child).children) {
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
            return FormulaFactory.mkX(FormulaFactory.mkG(((XOperator) child).operand)).acceptFormula(this);
        }
        return FormulaFactory.mkG(child);
    }

    public Formula visitL(Literal l) {
        return l;
    }

    public Formula visitN(Negation n) {
        if (n.operand instanceof Negation) {
            return ((Negation) n.operand).operand.acceptFormula(this);
        }
        if (n.operand instanceof BooleanConstant) {
            return FormulaFactory.mkConst(!((BooleanConstant) n.operand).get_value());
        } else if (n.operand instanceof Conjunction) {
            ArrayList<Formula> children = new ArrayList<Formula>();
            for (Formula grandchild : ((Conjunction) n.operand).children) {
                children.add(FormulaFactory.mkNot(grandchild));
            }
            return (FormulaFactory.mkOr(children)).acceptFormula(this);
        } else if (n.operand instanceof Disjunction) {
            ArrayList<Formula> children = new ArrayList<Formula>();
            for (Formula child : ((Disjunction) n.operand).children) {
                children.add(FormulaFactory.mkNot(child));
            }
            return (FormulaFactory.mkAnd(children)).acceptFormula(this);
        } else if (n.operand instanceof FOperator) {
            return (FormulaFactory.mkG(FormulaFactory.mkNot(((FOperator) n.operand).operand))).acceptFormula(this);
        } else if (n.operand instanceof GOperator) {
            return (FormulaFactory.mkF(FormulaFactory.mkNot(((GOperator) n.operand).operand))).acceptFormula(this);
        } else if (n.operand instanceof Literal) {
            return (((Literal) n.operand).negated());
        } else if (n.operand instanceof UOperator) {
            Formula child = n.operand.acceptFormula(this);
            if (!(child instanceof UOperator)) {

                return FormulaFactory.mkNot(child).acceptFormula(this);
            }
            return (FormulaFactory.mkOr(
                    FormulaFactory.mkU(FormulaFactory.mkNot(((UOperator) child).right),
                            FormulaFactory.mkAnd(FormulaFactory.mkNot(((UOperator) child).left),
                                    FormulaFactory.mkNot(((UOperator) child).right))),
                    FormulaFactory.mkG(FormulaFactory.mkNot(((UOperator) child).right)))).acceptFormula(this);
        } else if (n.operand instanceof XOperator) {
            return (FormulaFactory.mkX(FormulaFactory.mkNot(((XOperator) n.operand).operand)));
        }

        return n; // never happens
    }

    public Formula visitU(UOperator u) {
        if (u.right.acceptBool(Suspendable.getVisitor()) || u.right.acceptBool(EventualVisitor.getVisitor())) {
            return u.right.acceptFormula(this);
        } else {
            Formula l = u.left.acceptFormula(this);
            Formula r = u.right.acceptFormula(this);
            ImplicationVisitor imp = ImplicationVisitor.getVisitor();
            if (l.acceptBinarybool(imp, r)) {
                return r;
            } else if (FormulaFactory.mkNot(l).acceptFormula(this).acceptBinarybool(imp, r)) {
                return FormulaFactory.mkF(r).acceptFormula(this);
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
                return FormulaFactory.mkX(FormulaFactory.mkU(((XOperator) l).operand, ((XOperator) r).operand));
            }
            return FormulaFactory.mkU(l, r);
        }
    }

    public Formula visitX(XOperator x) {
        Formula child = x.operand.acceptFormula(this);
        if (child.acceptBool(Suspendable.getVisitor())) {
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
