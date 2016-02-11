package rabinizer.ltl;

import rabinizer.ltl.simplifier.Simplifier;

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
            Set<Formula> set = new HashSet<>(co.children);
            Set<Formula> toAdd = new HashSet<>();
            Set<Formula> toRemove = new HashSet<>();
            for (Formula form : set) {
                Formula f = form.accept(this, b, c);
                if (!f.equals(form)) {
                    toAdd.add(f);
                    toRemove.add(form);
                }
            }
            set.removeAll(toRemove);
            set.addAll(toAdd);
            if (!set.equals(co.children)) {
                return Simplifier.simplify(new Conjunction(set), Simplifier.Strategy.AGGRESSIVELY);
            }
            return co;
        }
    }

    @Override
    public Formula visit(Disjunction d, Formula b, Boolean c) {
        if (d.equals(b)) {
            return BooleanConstant.get(c);
        } else {
            Set<Formula> set = new HashSet<>(d.children);
            Set<Formula> toAdd = new HashSet<>();
            Set<Formula> toRemove = new HashSet<>();
            for (Formula form : set) {
                Formula f = form.accept(this, b, c);
                if (!f.equals(form)) {
                    toAdd.add(f);
                    toRemove.add(form);
                }
            }
            set.removeAll(toRemove);
            set.addAll(toAdd);

            if (!set.equals(d.children)) {
                return Simplifier.simplify(new Disjunction(set), Simplifier.Strategy.AGGRESSIVELY);
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
                if (g.operand.equals(b)) {
                    return BooleanConstant.get(c);
                }
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
        if (u.equals(b) || (u.right.equals(b) && c)) {
            return BooleanConstant.get(c);
        }
        return u;
    }

    @Override
    public Formula visit(XOperator x, Formula b, Boolean c) {
        if (x.equals(b)) {
            return BooleanConstant.get(c);
        } else if (b instanceof XOperator) {
            return new XOperator(x.operand.accept(this, ((XOperator) b).operand, c));
        }
        return x;
    }

}
