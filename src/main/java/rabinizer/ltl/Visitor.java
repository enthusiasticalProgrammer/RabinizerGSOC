package rabinizer.ltl;

public interface Visitor<R> {

    R defaultAction(Formula f);

    default R visit(BooleanConstant b) {
        return defaultAction(b);
    }

    default R visit(Conjunction c) {
        return defaultAction(c);
    }

    default R visit(Disjunction d) {
        return defaultAction(d);
    }

    default R visit(FOperator f) {
        return defaultAction(f);
    }

    default R visit(GOperator g) {
        return defaultAction(g);
    }

    default R visit(Literal l) {
        return defaultAction(l);
    }

    default R visit(UOperator u) {
        return defaultAction(u);
    }

    default R visit(XOperator x) {
        return defaultAction(x);
    }
}
