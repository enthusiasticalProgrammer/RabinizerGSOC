package rabinizer.ltl;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import net.sf.javabdd.BDD;
import rabinizer.ltl.bdd.BDDForFormulae;

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
        super(Arrays.asList(disjuncts));
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
            ArrayList<BoolExpr> exprs = new ArrayList<>();
            for (Formula child : children) {
                exprs.add(child.toExpr(ctx));
            }
            BoolExpr[] helper = new BoolExpr[exprs.size()];
            exprs.toArray(helper);
            cachedLTL = ctx.mkOr(helper);
        }
        return cachedLTL;

    }

    @Override
    public BDD bdd() {
        if (cachedBdd == null) {
            cachedBdd = BDDForFormulae.bddFactory.zero();
            for (Formula child : children) {
                cachedBdd = cachedBdd.or(child.bdd());
            }
            BDDForFormulae.representativeOfBdd(cachedBdd, this);
        }

        return cachedBdd;
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
        return children.stream().allMatch(c -> c.isPureEventual());
    }

    @Override
    public boolean isPureUniversal() {
        return children.stream().allMatch(c -> c.isPureUniversal());
    }

    @Override
    public boolean isSuspendable() {
        return children.stream().allMatch(c -> c.isSuspendable());
    }

}
