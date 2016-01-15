package rabinizer.ltl;

import org.jetbrains.annotations.NotNull;

public interface Visitor<R> {

    R defaultAction(@NotNull Formula formula);

    default R visit(@NotNull BooleanConstant booleanConstant) {
        return defaultAction(booleanConstant);
    }

    default R visit(@NotNull Conjunction conjunction) {
        return defaultAction(conjunction);
    }

    default R visit(@NotNull Disjunction disjunction) {
        return defaultAction(disjunction);
    }

    default R visit(@NotNull FOperator fOperator) {
        return defaultAction(fOperator);
    }

    default R visit(@NotNull GOperator gOperator) {
        return defaultAction(gOperator);
    }

    default R visit(@NotNull Literal literal) {
        return defaultAction(literal);
    }

    default R visit(@NotNull UOperator uOperator) {
        return defaultAction(uOperator);
    }

    default R visit(@NotNull XOperator xOperator) {
        return defaultAction(xOperator);
    }
}
