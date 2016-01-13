package rabinizer.ltl;

import java.util.HashSet;
import java.util.Set;

/**
 * 
 * this method tries to substitute the subformula b in the first argument by the
 * boolean constant specified by c, s.t. the returned formula is made up by
 * assuming the subformula b has the value c.
 *
 */
public class PseudoSubstitutionVisitor implements TripleVisitor<Formula, Formula, Boolean> {

    private static PseudoSubstitutionVisitor instance = new PseudoSubstitutionVisitor();

    private PseudoSubstitutionVisitor() {
    }

    public static PseudoSubstitutionVisitor getVisitor() {
        return instance;
    }

    @Override
    public Formula visit(BooleanConstant bo, Formula b, Boolean c) {
        return bo;
    }

    @Override
    public Formula visit(Conjunction co, Formula b, Boolean c) {
        if (co.equals(b)) {
            return BooleanConstant.get(c);
        } else {
            Set<Formula> set = new HashSet<>();
            set.addAll(co.children);
            for (Formula form : set) {
                Formula f = form.accept(this, b, c);
                if (!f.equals(form)) {
                    set.remove(form);
                    set.add(f);
                }
            }
            if (!set.equals(co.children)) {
                return Simplifier.simplify(new Conjunction(set), Simplifier.Strategy.PROPOSITIONAL);
            }
            return co;
        }
    }

    @Override
    public Formula visit(Disjunction d, Formula b, Boolean c) {
        if (d.equals(b)) {
            return BooleanConstant.get(c);
        } else {
            Set<Formula> set = new HashSet<>();
            set.addAll(d.children);
            for (Formula form : set) {
                Formula f = form.accept(this, b, c);
                if (!f.equals(form)) {
                    set.remove(form);
                    set.add(f);
                }
            }
            if (!set.equals(d.children)) {
                return Simplifier.simplify(new Disjunction(set), Simplifier.Strategy.PROPOSITIONAL);
            }
            return d;
        }
    }

    @Override
    public Formula visit(FOperator f, Formula b, Boolean c) {
        if (f.equals(b)) {
            return BooleanConstant.get(c);
        } else {
            if (c) {
                if (f.operand.equals(b)) {
                    return BooleanConstant.get(c);
                }
            }
            return f;
        }
    }

    @Override
    public Formula visit(GOperator g, Formula b, Boolean c) {
        if (g.equals(b)) {
            return BooleanConstant.get(c);
        } else {
            if (!c) {
                return new GOperator(g.operand.accept(this, b, c));
            }
        }
        return g;

    }

    @Override
    public Formula visit(Literal l, Formula b, Boolean c) {
        if (l.equals(b)) {
            return BooleanConstant.get(c);
        }
        return l;
    }

    @Override
    public Formula visit(UOperator u, Formula b, Boolean c) {
        if (u.equals(b) || u.right.equals(b)) {
            return BooleanConstant.get(c);
        }
        return u;
    }

    @Override
    public Formula visit(XOperator x, Formula b, Boolean c) {
        if (x.equals(b)) {
            return BooleanConstant.get(c);
        }
        return x;
    }

}
