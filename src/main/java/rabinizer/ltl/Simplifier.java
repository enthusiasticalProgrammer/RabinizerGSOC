package rabinizer.ltl;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.BaseStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Simplifier {

    public static final int ITERATIONS = 3;

    private static final Visitor<Formula> PROPOSITIONAL_SIMPLIFIER = new PropositionalSimplifier();
    private static final Visitor<Formula> MODAL_SIMPLIFIER = new ModalSimplifier();
    private static final Visitor<XFormula> PULLUP_X = new PullupXVisitor();
    private static final Visitor<Formula> PUSHDOWN_FG = new PushDownFGVisitor();

    private Simplifier() {
    }

    public static Formula simplify(Formula formula) {
        return Simplifier.simplify(formula, Strategy.MODAL);
    }

    public static Formula simplify(Formula formula, Strategy strategy) {
        switch (strategy) {
            case PULLUP_X:
                return formula.accept(PULLUP_X).toFormula();

            case MODAL_EXT:
                Formula step0 = formula;

                for (int i = 0; i < ITERATIONS; i++) {
                    Formula step1 = step0.accept(PUSHDOWN_FG);
                    Formula step2 = step1.accept(PULLUP_X).toFormula();
                    Formula step3 = step2.accept(MODAL_SIMPLIFIER);

                    if (step0.equals(step3)) {
                        return step0;
                    }

                    step0 = step3;
                }

                return step0;

            case MODAL:
                return formula.accept(Simplifier.MODAL_SIMPLIFIER);

            case PROPOSITIONAL:
            default:
                return formula.accept(Simplifier.PROPOSITIONAL_SIMPLIFIER);
        }
    }

    public enum Strategy {
        PROPOSITIONAL, MODAL, PULLUP_X, MODAL_EXT
    }

    static class PropositionalSimplifier implements Visitor<Formula> {
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
                    flattSet.addAll(((PropositionalFormula) child).children);
                } else {
                    flattSet.add(child);
                }
            }

            return flattSet;
        }

        @Override
        public Formula visit(Conjunction conjunction) {
            Stream<Formula> workStream = conjunction.children.stream().map(e -> e.accept(this));
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
            Stream<Formula> workStream = disjunction.children.stream().map(e -> e.accept(this));
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

    static class ModalSimplifier extends PropositionalSimplifier {
        @Override
        public Formula visit(FOperator fOperator) {
            Formula operand = fOperator.operand.accept(this);

            if (operand instanceof UOperator) {
                return new FOperator(((UOperator) operand).right);
            }

            if (operand.isPureEventual() || operand.isSuspendable()) {
                return operand;
            }

            return new FOperator(operand);
        }

        @Override
        public Formula visit(GOperator gOperator) {
            Formula operand = gOperator.operand.accept(this);

            if (operand.isPureUniversal() || operand.isSuspendable()) {
                return operand;
            }

            return new GOperator(operand);
        }

        @Override
        public Formula visit(UOperator uOperator) {
            Formula left = uOperator.left.accept(this);
            Formula right = uOperator.right.accept(this);

            if (right.isPureEventual() || right.isSuspendable()) {
                return right;
            }

            if (left.equals(BooleanConstant.TRUE)) {
                return new FOperator(right);
            }

            if (left.equals(BooleanConstant.FALSE)) {
                return right;
            }

            if (left.isPureUniversal()) {
                return new Disjunction(right, new Conjunction(left, new FOperator(right)));
            }

            return new UOperator(left, right);
        }

        @Override
        public Formula visit(XOperator xOperator) {
            Formula operand = xOperator.operand.accept(this);

            if (operand.isSuspendable()) {
                return operand;
            }

            return new XOperator(operand);
        }
    }

    static class XFormula {
        int depth;
        Formula formula;

        XFormula(int depth, Formula formula) {
            this.depth = depth;
            this.formula = formula;
        }

        Formula toFormula(int newDepth) {
            int i = depth - newDepth;

            for (; i > 0; i--) {
                formula = new XOperator(formula);
            }

            return formula;
        }

        Formula toFormula() {
            return toFormula(0);
        }
    }

    static class PullupXVisitor implements Visitor<XFormula> {
        @Override
        public XFormula defaultAction(Formula formula) {
            return new XFormula(0, formula);
        }

        @Override
        public XFormula visit(Conjunction conjunction) {
            Collection<XFormula> children = conjunction.children.stream().map(c -> c.accept(this)).collect(Collectors.toList());
            int depth = children.stream().mapToInt(c -> c.depth).min().orElse(0);
            return new XFormula(depth, new Conjunction(children.stream().map(c -> c.toFormula(depth))));
        }

        @Override
        public XFormula visit(Disjunction disjunction) {
            Collection<XFormula> children = disjunction.children.stream().map(c -> c.accept(this)).collect(Collectors.toList());
            int depth = children.stream().mapToInt(c -> c.depth).min().orElse(0);
            return new XFormula(depth, new Disjunction(children.stream().map(c -> c.toFormula(depth))));
        }

        @Override
        public XFormula visit(FOperator fOperator) {
            XFormula r = fOperator.operand.accept(this);
            r.formula = new FOperator(r.formula);
            return r;
        }

        @Override
        public XFormula visit(GOperator gOperator) {
            XFormula r = gOperator.operand.accept(this);
            r.formula = new GOperator(r.formula);
            return r;
        }

        @Override
        public XFormula visit(UOperator uOperator) {
            XFormula r = uOperator.right.accept(this);
            XFormula l = uOperator.left.accept(this);
            l.formula = new UOperator(l.toFormula(r.depth), r.toFormula(l.depth));
            l.depth = Math.min(l.depth, r.depth);
            return l;
        }

        @Override
        public XFormula visit(XOperator xOperator) {
            XFormula r = xOperator.operand.accept(this);
            r.depth++;
            return r;
        }
    }

    static class PushDownFGVisitor implements Visitor<Formula> {

        @Override
        public Formula defaultAction(Formula formula) {
            return formula;
        }

        @Override
        public Formula visit(Conjunction conjunction) {
            return new Conjunction(conjunction.children.stream().map(e -> e.accept(this)));
        }

        @Override
        public Formula visit(Disjunction disjunction) {
            return new Disjunction(disjunction.children.stream().map(e -> e.accept(this)));
        }

        @Override
        public Formula visit(FOperator fOperator) {
            if (fOperator.operand instanceof Disjunction) {
                Disjunction disjunction = (Disjunction) fOperator.operand;
                return new Disjunction(disjunction.children.stream().map(e -> new FOperator(e).accept(this)));
            }

            if (fOperator.operand instanceof UOperator) {
                UOperator uOperator = (UOperator) fOperator.operand;
                return new FOperator(uOperator.right).accept(this);
            }

            if (fOperator.operand instanceof FOperator) {
                return fOperator.operand.accept(this);
            }

            return new FOperator(fOperator.operand.accept(this));
        }

        @Override
        public Formula visit(GOperator gOperator) {
            if (gOperator.operand instanceof Conjunction) {
                Conjunction conjunction = (Conjunction) gOperator.operand;
                return new Conjunction(conjunction.children.stream().map(e -> new GOperator(e).accept(this)));
            }

            if (gOperator.operand instanceof GOperator) {
                return gOperator.operand.accept(this);
            }

            return new GOperator(gOperator.operand.accept(this));
        }

        @Override
        public Formula visit(UOperator uOperator) {
            return new UOperator(uOperator.left.accept(this), uOperator.right.accept(this));
        }

        @Override
        public Formula visit(XOperator xOperator) {
            return new XOperator(xOperator.operand.accept(this));
        }
    }
}
