package rabinizer.ltl;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class SimplifyBooleanVisitor implements Visitor<Formula> {

    private final static SimplifyBooleanVisitor instance = new SimplifyBooleanVisitor();

    public static SimplifyBooleanVisitor getVisitor() {
        return instance;
    }

    public static Formula simplify(Formula formula) {
        return formula.accept(instance);
    }

    private static Set<Formula> flatten(Stream<Formula> workStream, Predicate<PropositionalFormula> shouldUnfold, BooleanConstant breakC, BooleanConstant continueC) {
        Set<Formula> flattSet = new HashSet<>();
        Iterator<Formula> iter = workStream.iterator();

        while (iter.hasNext()) {
            Formula child = iter.next();

            if (breakC.equals(child)) {
                return Collections.singleton(breakC);
            }

            if (continueC.equals(child)) {
                continue;
            }

            if (child instanceof PropositionalFormula && shouldUnfold.test((PropositionalFormula) child)) {
                flattSet.addAll(((PropositionalFormula) child).getChildren());
            } else {
                flattSet.add(child);
            }
        }

        return flattSet;
    }

    @Override
    public Formula visit(Conjunction c) {
        Stream<Formula> workStream = c.getChildren().stream().map(e -> e.accept(this));
        Set<Formula> set = flatten(workStream, e -> e instanceof Conjunction, BooleanConstant.FALSE, BooleanConstant.TRUE);

        if (set.isEmpty()) {
            return BooleanConstant.TRUE;
        }

        if (set.size() == 1) {
            return set.iterator().next();
        }

        // This check may be to strong...
        if (set.stream().anyMatch(e -> set.contains(e.not()))) {
            return BooleanConstant.FALSE;
        }

        return new Conjunction(set);
    }

    @Override
    public Formula visit(Disjunction d) {
        Stream<Formula> workStream = d.getChildren().stream().map(e -> e.accept(this));
        Set<Formula> set = flatten(workStream, e -> e instanceof Disjunction, BooleanConstant.TRUE, BooleanConstant.FALSE);

        if (set.isEmpty()) {
            return BooleanConstant.FALSE;
        }

        if (set.size() == 1) {
            return set.iterator().next();
        }

        // This check may be to strong...
        if (set.stream().anyMatch(e -> set.contains(e.not()))) {
            return BooleanConstant.TRUE;
        }

        return new Disjunction(set);
    }

    @Override
    public Formula defaultAction(Formula f) {
        return f;
    }
}
