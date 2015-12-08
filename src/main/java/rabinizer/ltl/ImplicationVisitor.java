package rabinizer.ltl;

//visitY(a,b) returns true if a=>b, and if we don't know it or a doesn't imply b then false.
//it is highly recommended to have the formulae agressively simplified before
//the class is written to be used only in simplifyAggressively for a Con- or Disjunction
public class ImplicationVisitor implements BinaryVisitor<Boolean, Formula> {

    private static ImplicationVisitor instance = new ImplicationVisitor();

    private ImplicationVisitor() {
    }

    public static ImplicationVisitor getVisitor() {
        return instance;
    }

    public Boolean visit(BooleanConstant b, Formula fo) {
        if (b.value) {
            return b.equals(fo);
        } else {
            return true;
        }
    }

    public Boolean visit(Conjunction c, Formula fo) {
        if (c.equals(fo)) {
            return true;
        }
        if (fo instanceof Conjunction) {
            boolean imp = true;
            for (Formula fochild : ((PropositionalFormula) fo).children) {
                boolean impClause = false;
                for (Formula child : c.children) {
                    impClause = impClause || child.accept(this, fochild);
                }
                imp = imp && impClause;
            }
            return imp;
        } else {
            boolean imp = false;
            for (Formula child : c.children) {
                imp = imp || child.accept(this, fo);
            }
            return imp;
        }
    }

    public Boolean visit(Disjunction d, Formula fo) {
        boolean imp = true;
        for (Formula child : d.children) {
            imp = imp && child.accept(this, fo);
        }
        return imp;
    }

    public Boolean visit(FOperator f, Formula fo) {
        if (fo instanceof FOperator) {
            return f.operand.accept(this, ((ModalOperator) fo).operand);
        }
        return false;
    }

    public Boolean visit(GOperator g, Formula fo) {
        if (g.equals(fo)) {
            return true;
        }
        if (fo.equals(g.operand)) {
            return true;
        } else if (fo instanceof BooleanConstant) {
            return ((BooleanConstant) fo).value;
        } else if (fo instanceof Conjunction) {
            boolean imp = true;
            for (Formula fochild : ((PropositionalFormula) fo).children) {
                imp = imp && g.accept(this, fochild);
            }
            return imp;
        } else if (fo instanceof Disjunction) {
            boolean imp = false;
            for (Formula fochild : ((PropositionalFormula) fo).children) {
                imp = imp || g.accept(this, fochild);
            }
            return imp;
        } else if (fo instanceof FOperator || fo instanceof GOperator) {
            return g.accept(this, ((ModalOperator) fo).operand);
        } else if (fo instanceof Literal) {
            return g.operand.accept(this, fo);
        } else if (fo instanceof UOperator) {
            return g.accept(this, ((UOperator) fo).right) || g.accept(this, FormulaFactory
                    .mkAnd(FormulaFactory.mkG(((UOperator) fo).left), FormulaFactory.mkF(((UOperator) fo).right)));
        } else if (fo instanceof XOperator) {
            return g.accept(this, ((ModalOperator) fo).operand) || g.operand.accept(this, fo);
        }
        return false;
    }

    public Boolean visit(Literal l, Formula fo) {
        if (fo instanceof Conjunction) {
            boolean imp = true;
            for (Formula child : ((PropositionalFormula) fo).children) {
                imp = imp && l.accept(this, child);
            }
            return imp;
        } else if (fo instanceof Disjunction) {
            boolean imp = false;
            for (Formula child : ((PropositionalFormula) fo).children) {
                imp = imp || l.accept(this, child);
            }
        } else if (fo instanceof Literal) {
            return l.equals(fo);
        } else if (fo instanceof BooleanConstant) {
            return ((BooleanConstant) fo).value;
        } else if (fo instanceof FOperator) {
            return l.accept(this, ((ModalOperator) fo).operand);
        }
        return false;
    }

    public Boolean visit(UOperator u, Formula fo) {
        if (u.equals(fo)) {
            return true;
        }
        if (fo instanceof UOperator) {
            return u.left.accept(this, ((UOperator) fo).left) && u.right.accept(this, ((UOperator) fo).right);
        } else if (fo instanceof FOperator) {
            return u.right.accept(this, ((ModalOperator) fo).operand);
        } else {
            return FormulaFactory.mkAnd(u.left, u.right).accept(SimplifyAggressivelyVisitor.getVisitor()).accept(this,
                    fo);
        }

    }

    public Boolean visit(XOperator x, Formula fo) {
        if (x.equals(fo)) {
            return true;
        }
        if (fo instanceof BooleanConstant) {
            return ((BooleanConstant) fo).value;
        } else if (fo instanceof Conjunction) {
            boolean imp = true;
            for (Formula child : ((PropositionalFormula) fo).children) {
                imp = imp && x.accept(this, child);
            }
            return imp;
        } else if (fo instanceof Disjunction) {
            boolean imp = false;
            for (Formula child : ((PropositionalFormula) fo).children) {
                imp = imp || x.accept(this, child);
            }
            return imp;
        } else if (fo instanceof FOperator) {
            return x.operand.accept(this, fo) || x.accept(this, ((ModalOperator) fo).operand)
                    || x.operand.accept(this, ((ModalOperator) fo).operand);
        } else if (fo instanceof GOperator || fo instanceof Literal || fo instanceof UOperator) {
            return false;
        } else if (fo instanceof XOperator) {
            return x.operand.accept(this, ((ModalOperator) fo).operand);
        }
        return false;

    }

}
