package rabinizer.ltl;

import org.jetbrains.annotations.NotNull;

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

    private static final Visitor<Formula> AGGRESSIVE_SIMPLIFIER = new AggressiveSimplifier();

    private Simplifier() {
    }

    public static Formula simplify(Formula formula) {
        return Simplifier.simplify(formula, Strategy.MODAL);
    }

    public static Formula simplify(Formula formula, Strategy strategy) {
        switch (strategy) {

            case PROPOSITIONAL:
                return formula.accept(Simplifier.PROPOSITIONAL_SIMPLIFIER);

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

            case AGGRESSIVELY:
                return formula.accept(Simplifier.AGGRESSIVE_SIMPLIFIER);

            default:
                throw new AssertionError();

        }
    }

    public enum Strategy {

        PROPOSITIONAL, MODAL, PULLUP_X, MODAL_PULLUP_X, MODAL_EXT, AGGRESSIVELY

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
        public Formula visit(@NotNull Conjunction conjunction) {
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
        public Formula visit(@NotNull Disjunction disjunction) {
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
        public Formula defaultAction(@NotNull Formula formula) {
            return formula;
        }
    }

    static class ModalSimplifier extends PropositionalSimplifier {
        @Override
        public Formula visit(@NotNull FOperator fOperator) {
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
        public Formula visit(@NotNull GOperator gOperator) {
            Formula operand = gOperator.operand.accept(this);

            if (operand.isPureUniversal() || operand.isSuspendable()) {
                return operand;
            }

            return new GOperator(operand);
        }

        @Override
        public Formula visit(@NotNull UOperator uOperator) {
            Formula l = uOperator.left.accept(this);
            Formula r = uOperator.right.accept(this);

            if (r.isSuspendable() || r.isPureEventual()) {
                return r;
            }

            if (l.isSuspendable() || l.isPureUniversal()) {
                Formula f = new Conjunction(l, new FOperator(r));
                f = new Disjunction(f, r);
                return f;
            }

            if (l.isPureEventual()) {
                Formula f = new FOperator(new Conjunction(l, new XOperator(r)));
                f = new Disjunction(f, r);
                return f;
            }

            return new UOperator(l, r);
        }

        @Override
        public Formula visit(@NotNull XOperator xOperator) {
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
        public XFormula defaultAction(@NotNull Formula formula) {
            return new XFormula(0, formula);
        }

        @Override
        public XFormula visit(@NotNull Conjunction conjunction) {
            Collection<XFormula> children = conjunction.children.stream().map(c -> c.accept(this)).collect(Collectors.toList());
            int depth = children.stream().mapToInt(c -> c.depth).min().orElse(0);
            return new XFormula(depth, new Conjunction(children.stream().map(c -> c.toFormula(depth))));
        }

        @Override
        public XFormula visit(@NotNull Disjunction disjunction) {
            Collection<XFormula> children = disjunction.children.stream().map(c -> c.accept(this)).collect(Collectors.toList());
            int depth = children.stream().mapToInt(c -> c.depth).min().orElse(0);
            return new XFormula(depth, new Disjunction(children.stream().map(c -> c.toFormula(depth))));
        }

        @Override
        public XFormula visit(@NotNull FOperator fOperator) {
            XFormula r = fOperator.operand.accept(this);
            r.formula = new FOperator(r.formula);
            return r;
        }

        @Override
        public XFormula visit(@NotNull GOperator gOperator) {
            XFormula r = gOperator.operand.accept(this);
            r.formula = new GOperator(r.formula);
            return r;
        }

        @Override
        public XFormula visit(@NotNull UOperator uOperator) {
            XFormula r = uOperator.right.accept(this);
            XFormula l = uOperator.left.accept(this);
            l.formula = new UOperator(l.toFormula(r.depth), r.toFormula(l.depth));
            l.depth = Math.min(l.depth, r.depth);
            return l;
        }

        @Override
        public XFormula visit(@NotNull XOperator xOperator) {
            XFormula r = xOperator.operand.accept(this);
            r.depth++;
            return r;
        }
    }

    static class PushDownFGVisitor implements Visitor<Formula> {

        @Override
        public Formula defaultAction(@NotNull Formula formula) {
            return formula;
        }

        @Override
        public Formula visit(@NotNull Conjunction conjunction) {
            return new Conjunction(conjunction.children.stream().map(e -> e.accept(this)));
        }

        @Override
        public Formula visit(@NotNull Disjunction disjunction) {
            return new Disjunction(disjunction.children.stream().map(e -> e.accept(this)));
        }

        @Override
        public Formula visit(@NotNull FOperator fOperator) {
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
        public Formula visit(@NotNull GOperator gOperator) {
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
        public Formula visit(@NotNull UOperator uOperator) {
            return new UOperator(uOperator.left.accept(this), uOperator.right.accept(this));
        }

        @Override
        public Formula visit(@NotNull XOperator xOperator) {
            return new XOperator(xOperator.operand.accept(this));
        }
    }

    public static class AggressiveSimplifier extends ModalSimplifier implements Visitor<Formula> {
        @Override
        public Formula defaultAction(@NotNull Formula f) {
            return f; // for boolean constants and literals
        }

        @Override
        public Formula visit(@NotNull Conjunction c) {
            Formula con = super.visit(c);
            if (!(con instanceof Conjunction)) {
                return con;
            }
            c = (Conjunction) con;
            Set<Formula> set = new HashSet<>(c.children);

            // remove ltl that are implied by other Formulas
            // or do a PseudoSubstitution by a fix-point-iteration
            for (; innerConjunctionLoop(set);)
                ;

            return super.visit(new Conjunction(set));
        }

        /**
         * this method helps simplifyAgressively by performing one change of the
         * children set, and returning true, if something has changed
         */
        private boolean innerConjunctionLoop(Set<Formula> set) {

            Set<Formula> toAdd = new HashSet<>();


            Iterator<Formula> formula = set.iterator();
            while (formula.hasNext()) {
                Formula form = formula.next();
                Iterator<Formula> formula2 = set.iterator();
                while (formula2.hasNext()) {
                    Formula form2 = formula2.next();
                    if (!form.equals(form2)) {
                        ImplicationVisitor imp = ImplicationVisitor.getVisitor();
                        if (form.accept(imp, form2)) {
                            formula2.remove();
                            continue;
                        }

                        if (form.accept(imp, form2.not())) {
                            toAdd.add(BooleanConstant.FALSE);
                            break;
                        }

                        Formula f = form.accept(PseudoSubstitutionVisitor.getVisitor(), form2, true);
                        if (!f.equals(form)) {
                            formula.remove();
                            f = f.accept(this);
                            toAdd.add(f);
                            break;
                        }
                    }
                }
            }

            set.addAll(toAdd);
            return toAdd.isEmpty();
        }

        @Override
        public Formula visit(@NotNull Disjunction d) {
            Formula dis = super.visit(d);
            if (!(dis instanceof Disjunction)) {
                return dis;
            }
            d = ((Disjunction) dis);
            Set<Formula> set = new HashSet<>(d.children);

            // remove ltl that imply other Formulas
            // or do a PseudoSubstitution by a fix-point-iteration
            for (; innerDisjunctionLoop(set);)
                ;

            return super.visit(new Disjunction(set));
        }

        /**
         * this method helps simplifyAgressively by performing one change of the
         * children set, and returning true, if something has changed
         */
        private boolean innerDisjunctionLoop(Set<Formula> set) {

            Set<Formula> toAdd = new HashSet<>();

            Iterator<Formula> formula = set.iterator();

            while (formula.hasNext()) {
                Formula form = formula.next();
                Iterator<Formula> formula2 = set.iterator();
                while (formula2.hasNext()) {
                    Formula form2 = formula2.next();
                    if (!form.equals(form2)) {
                        ImplicationVisitor imp = ImplicationVisitor.getVisitor();
                        if (form.accept(imp, form2)) {
                            formula.remove();
                            break;
                        }

                        if (form.not().accept(imp, form2)) {
                            toAdd.add(BooleanConstant.TRUE);
                            break;
                        }

                        Formula f = form.accept(PseudoSubstitutionVisitor.getVisitor(), form2, false);
                        if (!f.equals(form)) {
                            formula.remove();
                            f = f.accept(this);
                            toAdd.add(f);
                            break;
                        }

                    }
                }
            }

            set.addAll(toAdd);
            return toAdd.isEmpty();
        }

        @Override
        public Formula visit(@NotNull FOperator f) {
            Formula newF = super.visit(f);
            if (newF instanceof FOperator) {
                Formula child = ((FOperator) newF).operand;
                if (child instanceof XOperator) {
                    return new XOperator(new FOperator(((ModalOperator) child).operand)).accept(this);
                }

                if (child instanceof Disjunction) {
                    return (new Disjunction(((PropositionalFormula) child).children.stream().map(FOperator::new))).accept(this);
                }
            }
            return newF;
        }

        @Override
        public Formula visit(@NotNull GOperator g) {
            Formula newG = super.visit(g);
            if (newG instanceof GOperator) {
                Formula child = ((GOperator) newG).operand;
                if (child.isPureUniversal() || child.isSuspendable()) {
                    return child;
                }

                if (child instanceof XOperator) {
                    return new XOperator(new GOperator(((ModalOperator) child).operand)).accept(this);
                }

                if (child instanceof Conjunction) {
                    return (new Conjunction(((PropositionalFormula) child).children.stream().map(GOperator::new))).accept(this);
                }

                if (child instanceof UOperator) {
                    Formula l = new GOperator(new Disjunction(((UOperator) child).left, ((UOperator) child).right));
                    Formula r = new GOperator(new FOperator(((UOperator) child).right));
                    return new Conjunction(l, r).accept(this);
                }
            }
            return newG;
        }

        @Override
        public Formula visit(@NotNull UOperator u) {

            Formula newU = super.visit(u);
            if (newU instanceof UOperator) {
                Formula l = ((UOperator) newU).left;
                Formula r = ((UOperator) newU).right;
                ImplicationVisitor imp = ImplicationVisitor.getVisitor();
                if (l.accept(imp, r) || r instanceof BooleanConstant) {
                    return r;
                }

                if (l instanceof BooleanConstant) {
                    if (((BooleanConstant) l).value) {
                        return new FOperator(r).accept(this);
                    } else {
                        return r;
                    }
                }

                if (l instanceof XOperator && r instanceof XOperator) {
                    return new XOperator(new UOperator(((ModalOperator) l).operand, ((ModalOperator) r).operand)).accept(this);
                }

                if (l instanceof Conjunction) {
                    return new Conjunction(((Conjunction) l).children.stream().map(left -> new UOperator(left, r)).collect(Collectors.toSet())).accept(this);
                }

                if (r instanceof Disjunction) {
                    return new Disjunction(((Disjunction) r).children.stream().map(right -> new UOperator(l, right)).collect(Collectors.toSet())).accept(this);
                }
            }
            return newU;
        }
    }
}
