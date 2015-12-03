package rabinizer.ltl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class SimplifyAggressivelyVisitor implements Visitor<Formula> {

    private static SimplifyAggressivelyVisitor instance = new SimplifyAggressivelyVisitor();

    private SimplifyAggressivelyVisitor() {
        super();
    }

    public static SimplifyAggressivelyVisitor getVisitor() {
        return instance;
    }

    public Formula visit(BooleanConstant b) {
        return b;
    }

    public Formula visit(Conjunction c) {
        // first of all, get all subformulae beyound Conjunction(e.g. for c and
        // (a and b)
        // I want a,b, and c, because you can simplify it more

        Set<Formula> set = c.getAllChildrenOfConjunction();
        Set<Formula> toRemove = new HashSet<Formula>();
        Set<Formula> toAdd = new HashSet<Formula>();

        for (Formula form : set) {
            Formula f = form.accept(this);
            if (f != form) {
                toAdd.add(f);
                toRemove.add(form);
            }

            if (f instanceof BooleanConstant) {
                if (!((BooleanConstant) f).value) {
                    return FormulaFactory.mkConst(false);
                }
                toRemove.add(f);
            }
        }

        set.removeAll(toRemove);
        set.addAll(toAdd);
        toRemove.clear();
        toAdd.clear();

        // remove ltl that are implied by other Formulas
        for (Formula form : set) {
            for (Formula form2 : set) {
                if (!form.equals(form2)) {
                    ImplicationVisitor imp = ImplicationVisitor.getVisitor();
                    if (form.accept(imp, form2)) {
                        toRemove.add(form2);
                        continue;
                    } else {

                        Formula f = form.accept(PseudoSubstitutionVisitor.getVisitor(), form2, true);
                        if (f != form) {
                            toRemove.add(form);
                            toAdd.add(form.accept(PseudoSubstitutionVisitor.getVisitor(), form2, true));
                        }

                    }
                }
            }
        }

        set.removeAll(toRemove);
        set.addAll(toAdd);
        toRemove.clear();
        toAdd.clear();

        return FormulaFactory.mkAnd(set);
    }

    public Formula visit(Disjunction d) {
        Set<Formula> set = d.getAllChildrenOfDisjunction();
        Set<Formula> toRemove = new HashSet<Formula>();
        Set<Formula> toAdd = new HashSet<Formula>();

        for (Formula form : set) {
            Formula f = form.accept(this);
            if (f != form) {
                toAdd.add(f);
                toRemove.add(form);
            }
            if (f instanceof BooleanConstant) {
                if (((BooleanConstant) f).value) {
                    return FormulaFactory.mkConst(true);
                }
                toRemove.add(f);
            }
        }

        set.removeAll(toRemove);
        set.addAll(toAdd);
        toRemove.clear();
        toAdd.clear();

        // remove Formulas that are implied by other ltl
        for (Formula form : set) {
            for (Formula form2 : set) {
                if (!form.equals(form2)) {
                    ImplicationVisitor imp = ImplicationVisitor.getVisitor();
                    if (form.accept(imp, form2)) {
                        toRemove.add(form2);
                        continue;
                    } else {
                        Formula f = form.accept(PseudoSubstitutionVisitor.getVisitor(), form2, false);
                        if (f != form) {
                            toRemove.add(form);
                            toAdd.add(form.accept(PseudoSubstitutionVisitor.getVisitor(), form2, false));
                        }
                    }
                }
            }
        }

        set.removeAll(toRemove);
        set.addAll(toAdd);
        toRemove.clear();
        toAdd.clear();

        return FormulaFactory.mkOr((Formula[]) set.toArray());
    }

    public Formula visit(FOperator f) {
        Formula child = f.operand.accept(this);
        if (child instanceof BooleanConstant) {
            return child;
        } else if (child instanceof UOperator) {
            return FormulaFactory.mkF(((UOperator) child).right).accept(this);
        } else if (child instanceof FOperator) {
            return child;
        } else if (child instanceof XOperator) {
            return FormulaFactory.mkX(FormulaFactory.mkF(((FormulaUnary) child).operand)).accept(this);
        } else if (child instanceof Disjunction) {
            ArrayList<Formula> newChildren = new ArrayList<>();
            for (Formula grandchild : ((PropositionalFormula) child).children) {
                newChildren.add(FormulaFactory.mkF(grandchild));
            }
            return FormulaFactory.mkOr(newChildren).accept(this);
        } else {
            return FormulaFactory.mkF(child);
        }
    }

    public Formula visit(GOperator g) {
        Formula child = g.operand.accept(this);
        if (child instanceof BooleanConstant) {
            return child;
        } else if (child instanceof GOperator) {
            return child;
        } else if (child instanceof XOperator) {
            return FormulaFactory.mkX(FormulaFactory.mkG(((FormulaUnary) child).operand)).accept(this);
        }
        return FormulaFactory.mkG(child);
    }

    public Formula visit(Literal l) {
        return l;
    }

    public Formula visit(UOperator u) {
        if (u.right.isSuspendable() || u.right.isPureEventual()) {
            return u.right.accept(this);
        } else {
            Formula l = u.left.accept(this);
            Formula r = u.right.accept(this);
            ImplicationVisitor imp = ImplicationVisitor.getVisitor();
            if (l.accept(imp, r)) {
                return r;
            } else if (r instanceof BooleanConstant) {
                return r;
            } else if (l instanceof BooleanConstant) {
                if (((BooleanConstant) l).value) {
                    return FormulaFactory.mkF(r).accept(this);
                } else {
                    return r;
                }
            } else if (r instanceof FOperator) {
                return r;
            } else if (l instanceof FOperator) {
                return FormulaFactory.mkOr(r, FormulaFactory.mkF(FormulaFactory.mkAnd(FormulaFactory.mkX(r), l)))
                        .accept(this);
            } else if (l.accept(imp, r)) {
                return r;
            } else if (l instanceof GOperator) {
                return FormulaFactory.mkOr(FormulaFactory.mkAnd(l, FormulaFactory.mkF(r)), r).accept(this);
            } else if (l instanceof XOperator && r instanceof XOperator) {
                return FormulaFactory.mkX(FormulaFactory.mkU(((FormulaUnary) l).operand, ((FormulaUnary) r).operand));
            }
            return FormulaFactory.mkU(l, r);
        }
    }

    public Formula visit(XOperator x) {
        Formula child = x.operand.accept(this);
        if (child.isSuspendable()) {
            return child;
        } else if (child.equals(x.operand)) {
            return x;
        } else if (child instanceof BooleanConstant) {
            return child;
        } else {
            return FormulaFactory.mkX(child);
        }
    }

}