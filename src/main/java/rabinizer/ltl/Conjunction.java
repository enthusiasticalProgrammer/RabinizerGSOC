package rabinizer.ltl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
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
}
