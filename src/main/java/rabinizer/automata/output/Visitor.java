package rabinizer.automata.output;

import jhoafparser.ast.Atom;
import jhoafparser.ast.BooleanExpression;

public interface Visitor<T extends Atom, R> {
    R visitTrue(BooleanExpression<T> b);

    R visitFalse(BooleanExpression<T> f);

    R visitNot(BooleanExpression<T> n);

    R visitOr(BooleanExpression<T> o);

    R visitAnd(BooleanExpression<T> a);

    R visitAtom(BooleanExpression<T> at);

    default R visit(BooleanExpression<T> b) {
        if (b.isNOT()) {
            return visitNot(b);
        } else if (b.isAND()) {
            return visitAnd(b);
        } else if (b.isOR()) {
            return visitOr(b);
        } else if (b.isAtom()) {
            return visitAtom(b);
        } else if (b.isTRUE()) {
            return visitTrue(b);
        } else if (b.isFALSE()) {
            return visitFalse(b);
        }
        throw new RuntimeException("never occuring case");
    }
}
