/*
 * Copyright (C) 2016  (See AUTHORS)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package rabinizer.ltl.simplifier;

import org.jetbrains.annotations.NotNull;
import rabinizer.ltl.*;

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
        PROPOSITIONAL, MODAL, PULLUP_X, MODAL_EXT, AGGRESSIVELY
    }

    static class PropositionalSimplifier implements Visitor<Formula> {
        private static Set<Formula> flatten(Stream<Formula> workStream, Predicate<PropositionalFormula> shouldUnfold,
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
            Formula left = uOperator.left.accept(this);
            Formula right = uOperator.right.accept(this);

            if (right.isSuspendable() || right.isPureEventual()) {
                return right;
            }

            if (left.equals(BooleanConstant.TRUE)) {
                return new FOperator(right);
            }

            if (left.equals(BooleanConstant.FALSE)) {
                return right;
            }

            if (left.isSuspendable() || left.isPureUniversal()) {
                return new Disjunction(new Conjunction(left, new FOperator(right)), right);
            }

            if (left.isPureEventual()) {
                return new Disjunction(new FOperator(new Conjunction(left, new XOperator(right))), right);
            }

            return new UOperator(left, right);
        }

        @Override
        public Formula visit(@NotNull XOperator xOperator) {
            Formula operand = xOperator.operand.accept(this);

            if (operand.isSuspendable()) {
                return operand;
            }

            return new XOperator(operand);
        }

        @Override
        public Formula visit(@NotNull Conjunction conjunction) {
            Formula c = super.visit(conjunction);

            if (c instanceof Conjunction) {
                Conjunction c2 = (Conjunction) c;

                if (c2.children.stream().anyMatch(e -> c2.children.contains(e.not()))) {
                    return BooleanConstant.FALSE;
                }
            }

            return c;
        }

        @Override
        public Formula visit(@NotNull Disjunction disjunction) {
            Formula d = super.visit(disjunction);

            if (d instanceof Disjunction) {
                Disjunction d2 = (Disjunction) d;

                if (d2.children.stream().anyMatch(e -> d2.children.contains(e.not()))) {
                    return BooleanConstant.TRUE;
                }
            }

            return d;
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

    public static class AggressiveSimplifier extends ModalSimplifier {

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
            for (; innerConjunctionLoop(set); )
                ;

            Formula f = Simplifier.simplify(c, Strategy.PULLUP_X);
            if (f instanceof Conjunction) {
                return f;
            }

            return f.accept(this);
        }

        @Override
        public Formula visit(@NotNull Disjunction d) {
            Formula dis = super.visit(d);
            if (!(dis instanceof Disjunction)) {
                return dis;
            }

            d = (Disjunction) dis;
            Set<Formula> set = new HashSet<>(d.children);

            // remove ltl that imply other Formulas
            // or do a PseudoSubstitution by a fix-point-iteration
            while (innerDisjunctionLoop(set)) {
            }

            Formula f = Simplifier.simplify(d, Strategy.PULLUP_X);
            if (f instanceof Disjunction) {
                return f;
            }

            return f.accept(this);
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
                        return Simplifier.simplify(new FOperator(r), Strategy.PULLUP_X);
                    } else {
                        return r;
                    }
                }

                if (l instanceof XOperator && r instanceof XOperator) {
                    return Simplifier.simplify(new XOperator(new UOperator(((ModalOperator) l).operand, ((ModalOperator) r).operand)), Strategy.PULLUP_X);
                }

                if (l instanceof Conjunction) {
                    return Simplifier.simplify(new Conjunction(((Conjunction) l).children.stream().map(left -> new UOperator(left, r)).collect(Collectors.toSet())),
                            Strategy.PULLUP_X);
                }

                if (r instanceof Disjunction) {
                    Simplifier.simplify(new Disjunction(((Disjunction) r).children.stream().map(right -> new UOperator(l, right)).collect(Collectors.toSet())), Strategy.PULLUP_X);
                }
            }

            return newU;
        }

        /**
         * this method helps simplifyAgressively by performing one change of the
         * children set, and returning true, if something has changed
         */
        private boolean innerConjunctionLoop(Set<Formula> set) {
            Iterator<Formula> formula = set.iterator();

            while (formula.hasNext()) {
                Formula form = formula.next();
                Iterator<Formula> formula2 = set.iterator();
                while (formula2.hasNext()) {
                    Formula form2 = formula2.next();
                    if (!form.equals(form2)) {
                        ImplicationVisitor imp = ImplicationVisitor.getVisitor();

                        if (form.accept(imp, form2)) {
                            if (set.remove(form2))
                                return true;
                        }

                        if (form.accept(imp, form2.not())) {
                            set.clear();
                            set.add(BooleanConstant.FALSE);
                            return true;
                        }

                        Formula f = form.accept(PseudoSubstitutionVisitor.getVisitor(), form2, true);

                        if (!f.equals(form)) {
                            boolean possibleResult = set.remove(form);
                            set.remove(form);
                            f = f.accept(this);
                            possibleResult = set.add(f) || possibleResult;
                            if (possibleResult) {
                                return true;
                            }
                        }
                    }
                }
            }

            return false;
        }

        /**
         * this method helps simplifyAgressively by performing one change of the
         * children set, and returning true, if something has changed
         */
        private boolean innerDisjunctionLoop(Set<Formula> set) {
            Iterator<Formula> formula = set.iterator();

            while (formula.hasNext()) {
                Formula form = formula.next();
                for (Formula form2 : set) {
                    if (!form.equals(form2)) {
                        ImplicationVisitor imp = ImplicationVisitor.getVisitor();

                        if (form.accept(imp, form2)) {
                            if (set.remove(form))
                                return true;
                        }

                        if (form.not().accept(imp, form2)) {
                            set.clear();
                            set.add(BooleanConstant.TRUE);
                            return true;
                        }

                        Formula f = form.accept(PseudoSubstitutionVisitor.getVisitor(), form2, false);

                        if (!f.equals(form)) {
                            boolean possibleResult = set.remove(form);
                            f = f.accept(this);
                            possibleResult = set.add(f) || possibleResult;
                            if (possibleResult) {
                                return true;
                            }
                        }
                    }
                }
            }

            return false;
        }
    }
}
