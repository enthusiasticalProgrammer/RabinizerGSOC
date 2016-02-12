package rabinizer.automata.output;

import jhoafparser.ast.Atom;
import jhoafparser.ast.BooleanExpression;

public class RemoveConstants<T extends Atom> implements Visitor<T, BooleanExpression<T>> {

    @Override
    public BooleanExpression<T> visitTrue(BooleanExpression<T> b) {
        return b;
    }

    @Override
    public BooleanExpression<T> visitFalse(BooleanExpression<T> f) {
        return f;
    }

    @Override
    public BooleanExpression<T> visitNot(BooleanExpression<T> n) {
        throw new UnsupportedOperationException();

    }

    @Override
    public BooleanExpression<T> visitOr(BooleanExpression<T> o) {
        BooleanExpression<T> l = visit(o.getLeft());
        BooleanExpression<T> r = visit(o.getRight());
        if (l.isTRUE() || r.isTRUE()) {
            return new BooleanExpression<>(BooleanExpression.Type.EXP_TRUE, null, null);
        } else if (l.isFALSE()) {
            return r;
        } else if (r.isFALSE()) {
            return l;
        } else {
            return new BooleanExpression<>(BooleanExpression.Type.EXP_OR, l, r);
        }
    }

    @Override
    public BooleanExpression<T> visitAnd(BooleanExpression<T> a) {
        BooleanExpression<T> l = visit(a.getLeft());
        BooleanExpression<T> r = visit(a.getRight());
        if (l.isFALSE() || r.isFALSE()) {
            return new BooleanExpression<>(BooleanExpression.Type.EXP_FALSE, null, null);
        } else if (l.isTRUE()) {
            return r;
        } else if (r.isTRUE()) {
            return l;
        } else {
            return new BooleanExpression<>(BooleanExpression.Type.EXP_AND, l, r);
        }
    }

    @Override
    public BooleanExpression<T> visitAtom(BooleanExpression<T> at) {
        return at;
    }

}
