package rabinizer.ltl;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Andreas Gaiser & Ruslan Ledesma-Garza & Christopher Ziegler
 */
public final class Disjunction extends PropositionalFormula {

    public Disjunction(Collection<Formula> disjuncts) {
        super(disjuncts);
    }

    public Disjunction(Formula... disjuncts) {
        super(disjuncts);
    }

    @Override
    public Formula ThisTypeBoolean(Collection<Formula> af) {
        return FormulaFactory.mkOr(af);
    }

    @Override
    public String operator() {
        return "|";
    }

    @Override
    public Formula not() {
        return new Conjunction(children.stream().map(Formula::not).collect(Collectors.toSet()));
    }

    // ============================================================
    
    @Override
    public BoolExpr toExpr(Context ctx) {
        if (cachedLTL == null) {
            List<BoolExpr> exprs = children.stream().map(child -> child.toExpr(ctx)).collect(Collectors.toList());
            BoolExpr[] helper = new BoolExpr[exprs.size()];
            exprs.toArray(helper);
            cachedLTL = ctx.mkOr(helper);
        }
        return cachedLTL;

    }

    // helps the SimplifyBooleanVisitor
    protected Set<Formula> getAllChildrenOfDisjunction() {
        Set<Formula> al = new HashSet<>();

        for (Formula child : children) {
            if (child instanceof Disjunction) {
                al.addAll(((Disjunction) child).getAllChildrenOfDisjunction());
            } else {
                al.add(child);
            }
        }

        return al;
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }

    @Override
    public <A, B> A accept(BinaryVisitor<A, B> v, B f) {
        return v.visit(this, f);
    }

    @Override
    public <A, B, C> A accept(TripleVisitor<A, B, C> v, B f, C c) {
        return v.visit(this, f, c);
    }

    @Override
    public boolean isPureEventual() {
        return children.stream().allMatch(Formula::isPureEventual);
    }

    @Override
    public boolean isPureUniversal() {
        return children.stream().allMatch(Formula::isPureUniversal);
    }

    @Override
    public boolean isSuspendable() {
        return children.stream().allMatch(Formula::isSuspendable);
    }

}
