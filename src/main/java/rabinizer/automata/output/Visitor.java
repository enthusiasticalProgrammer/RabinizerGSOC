package rabinizer.automata.output;

import jhoafparser.ast.BooleanExpression;

public interface Visitor<R> {
    R visitTrue(BooleanExpression b);

    R visitFalse(BooleanExpression f);

    R visitNot(BooleanExpression n);

    R visitOr(BooleanExpression o);

    R visitAnd(BooleanExpression a);

    R visitAtom(BooleanExpression at);

    default R visit(BooleanExpression b) {
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
