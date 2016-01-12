package rabinizer.automata.output;

import jhoafparser.ast.BooleanExpression;

public class RemoveConstants implements Visitor<BooleanExpression> {

    @Override
    public BooleanExpression visitTrue(BooleanExpression b) {
        return b;
    }

    @Override
    public BooleanExpression visitFalse(BooleanExpression f) {
        return f;
    }

    @Override
    public BooleanExpression visitNot(BooleanExpression n) {
        throw new UnsupportedOperationException();

    }

    @Override
    public BooleanExpression visitOr(BooleanExpression o) {
        BooleanExpression l = visit(o.getLeft());
        BooleanExpression r = visit(o.getRight());
        if (l.isTRUE() || r.isTRUE()) {
            return HOAConsumerExtended.TRUE;
        } else if (l.isFALSE()) {
            return r;
        } else if (r.isFALSE()) {
            return l;
        } else {
            return new BooleanExpression(BooleanExpression.Type.EXP_OR, l, r);
        }
    }

    @Override
    public BooleanExpression visitAnd(BooleanExpression a) {
        BooleanExpression l = visit(a.getLeft());
        BooleanExpression r = visit(a.getRight());
        if (l.isFALSE() || r.isFALSE()) {
            return HOAConsumerExtended.FALSE;
        } else if (l.isTRUE()) {
            return r;
        } else if (r.isTRUE()) {
            return l;
        } else {
            return new BooleanExpression(BooleanExpression.Type.EXP_AND, l, r);
        }
    }

    @Override
    public BooleanExpression visitAtom(BooleanExpression at) {
        return at;
    }

}
