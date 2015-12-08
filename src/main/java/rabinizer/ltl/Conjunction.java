package rabinizer.ltl;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Andreas Gaiser & Ruslan Ledesma-Garza & Christopher Ziegler
 */
public final class Conjunction extends PropositionalFormula {

    public Conjunction(Collection<? extends Formula> conjuncts) {
        super(conjuncts);
    }

    public Conjunction(Formula... conjuncts) {
        super(conjuncts);
    }

    public Conjunction(Stream<? extends Formula> formulaStream) {
        super(formulaStream);
    }

    @Deprecated
    @Override
    public BoolExpr toExpr(Context ctx) {
        if (cachedLTL == null) {
            List<BoolExpr> exprs = children.stream().map(child -> child.toExpr(ctx)).collect(Collectors.toList());
            BoolExpr[] helper = new BoolExpr[exprs.size()];
            exprs.toArray(helper);
            cachedLTL = ctx.mkAnd(helper);
        }
        return cachedLTL;
    }

    @Override
    public Formula not() {
        return new Disjunction(children.stream().map(Formula::not));
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
    protected char getOperator() {
        return '&';
    }

    @Override
    protected PropositionalFormula create(Stream<? extends Formula> formulaStream) {
        return new Conjunction(formulaStream);
    }

    /**
     * helps the SimplifyBooleanVisitor
     *
     * @return every non-conjunction child of this Conjunction, and the children
     * of the Conjunction-children
     */
    protected Set<Formula> getAllChildrenOfConjunction() {
        Set<Formula> al = new HashSet<>(children.size());

        for (Formula child : children) {
            if (child instanceof Conjunction) {
                al.addAll(((Conjunction) child).getAllChildrenOfConjunction());
            } else {
                al.add(child);
            }
        }

        return al;
    }
}
