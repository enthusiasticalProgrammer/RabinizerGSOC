package rabinizer.ltl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.BaseStream;
import java.util.stream.Stream;

public final class Simplifier {

    private static final Visitor<Formula> PROPOSITIONAL_SIMPLIFIER = new PropositionalSimplifier();
    private static final Visitor<Formula> MODAL_SIMPLIFIER = new ModalSimplifier();

    private Simplifier() {
    }

    public static Formula simplify(Formula formula) {
        return Simplifier.simplify(formula, Strategy.MODAL);
    }

    public static Formula simplify(Formula formula, Strategy strategy) {
        switch (strategy) {
            case PROPOSITIONAL:
                return formula.accept(Simplifier.PROPOSITIONAL_SIMPLIFIER);

            case MODAL:
            default:
                return formula.accept(Simplifier.MODAL_SIMPLIFIER);
        }
    }

    public enum Strategy {
        PROPOSITIONAL, MODAL
    }

    private static class PropositionalSimplifier implements Visitor<Formula> {
        private static Set<Formula> flatten(BaseStream workStream, Predicate<PropositionalFormula> shouldUnfold,
                                            BooleanConstant breakC, BooleanConstant continueC) {
            Set<Formula> flattSet = new HashSet<>();
            Iterator<Formula> iterator = workStream.iterator();

            while (iterator.hasNext()) {
                Formula child = iterator.next();

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
        public Formula visit(Conjunction conjunction) {
            Stream<Formula> workStream = conjunction.getChildren().stream().map(e -> e.accept(this));
            Set<Formula> set = PropositionalSimplifier.flatten(workStream, e -> e instanceof Conjunction, BooleanConstant.FALSE,
                    BooleanConstant.TRUE);

            if (set.isEmpty()) {
                return BooleanConstant.TRUE;
            }

            if (set.size() == 1) {
                return set.iterator().next();
            }

            if (set.stream().anyMatch(e -> set.contains(e.not()))) {
                return BooleanConstant.FALSE;
            }

            return new Conjunction(set);
        }

        @Override
        public Formula visit(Disjunction disjunction) {
            Stream<Formula> workStream = disjunction.getChildren().stream().map(e -> e.accept(this));
            Set<Formula> set = PropositionalSimplifier.flatten(workStream, e -> e instanceof Disjunction, BooleanConstant.TRUE,
                    BooleanConstant.FALSE);

            if (set.isEmpty()) {
                return BooleanConstant.FALSE;
            }

            if (set.size() == 1) {
                return set.iterator().next();
            }

            if (set.stream().anyMatch(e -> set.contains(e.not()))) {
                return BooleanConstant.TRUE;
            }

            return new Disjunction(set);
        }

        @Override
        public Formula defaultAction(Formula formula) {
            return formula;
        }
    }

    private static class ModalSimplifier extends PropositionalSimplifier {
        @Override
        public Formula visit(FOperator fOperator) {
            Formula operand = fOperator.operand.accept(this);

            if (operand instanceof BooleanConstant || operand instanceof FOperator) {
                return operand;
            }

            return new FOperator(operand);
        }

        @Override
        public Formula visit(GOperator gOperator) {
            Formula operand = gOperator.operand.accept(this);

            if (operand instanceof BooleanConstant || operand instanceof GOperator) {
                return operand;
            }

            return new GOperator(operand);
        }

        @Override
        public Formula visit(UOperator uOperator) {
            Formula left = uOperator.left.accept(this);
            Formula right = uOperator.right.accept(this);

            if (right instanceof BooleanConstant) {
                return right;
            }

            if (left.equals(BooleanConstant.TRUE)) {
                return new FOperator(right);
            }

            if (left.equals(BooleanConstant.FALSE)) {
                return right;
            }

            return new UOperator(left, right);
        }

        @Override
        public Formula visit(XOperator xOperator) {
            Formula operand = xOperator.operand.accept(this);

            if (operand instanceof BooleanConstant) {
                return operand;
            }

            return new XOperator(operand);
        }
    }
}
