package rabinizer.ltl;

public interface Visitor<R> {

    R defaultAction(Formula formula);

    default R visit(BooleanConstant booleanConstant) {
        return defaultAction(booleanConstant);
    }

    default R visit(Conjunction conjunction) {
        return defaultAction(conjunction);
    }

    default R visit(Disjunction disjunction) {
        return defaultAction(disjunction);
    }

    default R visit(FOperator fOperator) {
        return defaultAction(fOperator);
    }

    default R visit(GOperator gOperator) {
        return defaultAction(gOperator);
    }

    default R visit(Literal literal) {
        return defaultAction(literal);
    }

    default R visit(UOperator uOperator) {
        return defaultAction(uOperator);
    }

    default R visit(XOperator xOperator) {
        return defaultAction(xOperator);
    }
}
