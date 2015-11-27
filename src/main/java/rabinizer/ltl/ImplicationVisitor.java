package rabinizer.ltl;

//visitY(a,b) returns true if a=>b, and if we don't know it or a doesn't imply b then false.
//it is highly recommended to have the formulae agressively simplified before
//the class is written to be used only in simplifyAggressively for a Con- or Disjunction
public class ImplicationVisitor implements AttributeBinaryVisitor {

    private static ImplicationVisitor instance = new ImplicationVisitor();

    private ImplicationVisitor() {
        super();
    }

    public static ImplicationVisitor getVisitor() {
        return instance;
    }

    public boolean visitB(BooleanConstant b, Formula fo) {
        if (b.get_value()) {
            return b.equals(fo);
        } else {
            return true;
        }
    }

    public boolean visitC(Conjunction c, Formula fo) {
        if (c.get_id() == fo.get_id()) {
            return true;
        }
        if (fo instanceof Conjunction) {
            boolean imp = true;
            for (Formula fochild : ((FormulaBinaryBoolean) fo).children) {
                boolean impClause = false;
                for (Formula child : c.children) {
                    impClause = impClause || child.acceptBinarybool(this, fochild);
                }
                imp = imp && impClause;
            }
            return imp;
        } else {
            boolean imp = false;
            for (Formula child : c.children) {
                imp = imp || child.acceptBinarybool(this, fo);
            }
            return imp;
        }
    }

    public boolean visitD(Disjunction d, Formula fo) {
        if (d.get_id() == fo.get_id()) {
            return true;
        }
        boolean imp = true;
        for (Formula child : d.children) {
            imp = imp && child.acceptBinarybool(this, fo);
        }
        return imp;
    }

    public boolean visitF(FOperator f, Formula fo) {
        if (f.get_id() == fo.get_id()) {
            return true;
        }
        if (fo instanceof FOperator) {
            return f.operand.acceptBinarybool(this, ((FormulaUnary) fo).operand);
        }
        return false;
    }

    public boolean visitG(GOperator g, Formula fo) {
        if (g.get_id() == fo.get_id()) {
            return true;
        }
        if (fo.get_id() == g.operand.get_id()) {
            return true;
        } else if (fo instanceof BooleanConstant) {
            return ((BooleanConstant) fo).get_value();
        } else if (fo instanceof Conjunction) {
            boolean imp = true;
            for (Formula fochild : ((FormulaBinaryBoolean) fo).children) {
                imp = imp && g.acceptBinarybool(this, fochild);
            }
            return imp;
        } else if (fo instanceof Disjunction) {
            boolean imp = false;
            for (Formula fochild : ((FormulaBinaryBoolean) fo).children) {
                imp = imp || g.acceptBinarybool(this, fochild);
            }
            return imp;
        } else if (fo instanceof FOperator) {
            return g.acceptBinarybool(this, ((FormulaUnary) fo).operand);
        } else if (fo instanceof GOperator) {
            return g.acceptBinarybool(this, ((FormulaUnary) fo).operand);
        } else if (fo instanceof Literal) {
            return g.operand.acceptBinarybool(this, fo);
        } else if (fo instanceof UOperator) {
            return g.acceptBinarybool(this, ((FormulaBinary) fo).right) || g.acceptBinarybool(this, FormulaFactory
                    .mkAnd(FormulaFactory.mkG(((FormulaBinary) fo).left), FormulaFactory.mkF(((FormulaBinary) fo).right)));
        } else if (fo instanceof XOperator) {
            return g.acceptBinarybool(this, ((FormulaUnary) fo).operand) || g.operand.acceptBinarybool(this, fo);
        }
        return false;
    }

    public boolean visitL(Literal l, Formula fo) {
        if (fo instanceof Conjunction) {
            boolean imp = true;
            for (Formula child : ((FormulaBinaryBoolean) fo).children) {
                imp = imp && l.acceptBinarybool(this, child);
            }
            return imp;
        } else if (fo instanceof Disjunction) {
            boolean imp = false;
            for (Formula child : ((FormulaBinaryBoolean) fo).children) {
                imp = imp || l.acceptBinarybool(this, child);
            }
        } else if (fo instanceof Literal) {
            return l.equals(fo);
        } else if (fo instanceof BooleanConstant) {
            return ((BooleanConstant) fo).get_value();
        } else if (fo instanceof FOperator) {
            return l.acceptBinarybool(this, ((FormulaUnary) fo).operand);
        }
        return false;
    }

    public boolean visitU(UOperator u, Formula fo) {
        if (u.get_id() == fo.get_id()) {
            return true;
        }
        if (fo instanceof UOperator) {
            return u.left.acceptBinarybool(this, ((FormulaBinary) fo).left)
                    && u.right.acceptBinarybool(this, ((FormulaBinary) fo).right);
        } else if (fo instanceof FOperator) {
            return u.right.acceptBinarybool(this, ((FormulaUnary) fo).operand);
        } else {
            return FormulaFactory.mkAnd(u.left, u.right).acceptFormula(SimplifyAggressivelyVisitor.getVisitor())
                    .acceptBinarybool(this, fo);
        }

    }

    public boolean visitX(XOperator x, Formula fo) {
        if (x.get_id() == fo.get_id()) {
            return true;
        }
        if (fo instanceof BooleanConstant) {
            return ((BooleanConstant) fo).get_value();
        } else if (fo instanceof Conjunction) {
            boolean imp = true;
            for (Formula child : ((FormulaBinaryBoolean) fo).children) {
                imp = imp && x.acceptBinarybool(this, child);
            }
            return imp;
        } else if (fo instanceof Disjunction) {
            boolean imp = false;
            for (Formula child : ((FormulaBinaryBoolean) fo).children) {
                imp = imp || x.acceptBinarybool(this, child);
            }
            return imp;
        } else if (fo instanceof FOperator) {
            return x.operand.acceptBinarybool(this, fo) || x.acceptBinarybool(this, ((FormulaUnary) fo).operand)
                    || x.operand.acceptBinarybool(this, ((FormulaUnary) fo).operand);
        } else if (fo instanceof GOperator) {
            return false;
        } else if (fo instanceof Literal) {
            return false;
        } else if (fo instanceof UOperator) {
            return false;
        } else if (fo instanceof XOperator) {
            return x.operand.acceptBinarybool(this, ((FormulaUnary) fo).operand);
        }
        return false;

    }

}
