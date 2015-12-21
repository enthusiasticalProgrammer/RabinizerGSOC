package rabinizer.ltl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author Andreas Gaiser & Ruslan Ledesma-Garza & Christopher Ziegler
 */
public final class Disjunction extends PropositionalFormula {

    public Disjunction(Collection<? extends Formula> disjuncts) {
        super(disjuncts);
    }

    public Disjunction(Formula... disjuncts) {
        super(disjuncts);
    }

    public Disjunction(Stream<? extends Formula> formulaStream) {
        super(formulaStream);
    }

    @Override
    public Formula not() {
        return new Conjunction(children.stream().map(Formula::not));
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
        return '|';
    }

    @Override
    protected PropositionalFormula create(Stream<? extends Formula> formulaStream) {
        return new Disjunction(formulaStream);
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
}
