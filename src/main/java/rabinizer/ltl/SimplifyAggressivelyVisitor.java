package rabinizer.ltl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class SimplifyAggressivelyVisitor implements Visitor<Formula> {

    private static SimplifyAggressivelyVisitor instance = new SimplifyAggressivelyVisitor();

    private SimplifyAggressivelyVisitor() {
    }

    public static SimplifyAggressivelyVisitor getVisitor() {
        return instance;
    }

    @Override
    public Formula defaultAction(Formula f) {
        return f;
    }

    @Override
    public Formula visit(Conjunction c) {

        Set<Formula> set = c.getAllChildrenOfConjunction();
        Set<Formula> toRemove = new HashSet<>();
        Set<Formula> toAdd = new HashSet<>();

        for (Formula form : set) {
            Formula f = form.accept(this);
            if (f != form) {
                toAdd.add(f);
                toRemove.add(form);
            }

            if (f instanceof BooleanConstant) {
                if (!((BooleanConstant) f).value) {
                    return BooleanConstant.get(false);
                }
                toRemove.add(f);
            }
        }

        set.removeAll(toRemove);
        set.addAll(toAdd);
        toRemove.clear();
        toAdd.clear();

        // remove ltl that are implied by other Formulas
        // TODO: first: PseudoSubstitution, then Implication
        for (Formula form : set) {
            for (Formula form2 : set) {
                if (!form.equals(form2)) {
                    ImplicationVisitor imp = ImplicationVisitor.getVisitor();
                    if (form.accept(imp, form2)) {
                        toRemove.add(form2);
                        continue;
                    } else {

                        Formula f = form.accept(PseudoSubstitutionVisitor.getVisitor(), form2, true);
                        if (f != form) {
                            toRemove.add(form);
                            toAdd.add(form.accept(PseudoSubstitutionVisitor.getVisitor(), form2, true));
                        }

                    }
                }
            }
        }

        set.removeAll(toRemove);
        set.addAll(toAdd);
        toRemove.clear();
        toAdd.clear();

        return Simplifier.simplify(new Conjunction(set), Simplifier.Strategy.PROPOSITIONAL);
    }

    @Override
    public Formula visit(Disjunction d) {
        Set<Formula> set = d.getAllChildrenOfDisjunction();
        Set<Formula> toRemove = new HashSet<>();
        Set<Formula> toAdd = new HashSet<>();

        for (Formula form : set) {
            Formula f = form.accept(this);
            if (f != form) {
                toAdd.add(f);
                toRemove.add(form);
            }
            if (f instanceof BooleanConstant) {
                if (((BooleanConstant) f).value) {
                    return BooleanConstant.get(true);
                }
                toRemove.add(f);
            }
        }

        set.removeAll(toRemove);
        set.addAll(toAdd);
        toRemove.clear();
        toAdd.clear();

        // remove Formulas that are implied by other ltl
        for (Formula form : set) {
            for (Formula form2 : set) {
                if (!form.equals(form2)) {
                    ImplicationVisitor imp = ImplicationVisitor.getVisitor();
                    if (form.accept(imp, form2)) {
                        toRemove.add(form2);
                        continue;
                    } else {
                        Formula f = form.accept(PseudoSubstitutionVisitor.getVisitor(), form2, false);
                        if (f != form) {
                            toRemove.add(form);
                            toAdd.add(form.accept(PseudoSubstitutionVisitor.getVisitor(), form2, false));
                        }
                    }
                }
            }
        }

        set.removeAll(toRemove);
        set.addAll(toAdd);
        toRemove.clear();
        toAdd.clear();

        return Simplifier.simplify(new Disjunction(set), Simplifier.Strategy.PROPOSITIONAL);
    }

    @Override
    public Formula visit(FOperator f) {
        Formula child = f.operand.accept(this);
        if (child instanceof BooleanConstant) {
            return child;
        } else if (child instanceof UOperator) {
            return new FOperator(((UOperator) child).right).accept(this);
        } else if (child instanceof FOperator) {
            return child;
        } else if (child instanceof XOperator) {
            return new XOperator(new FOperator(((ModalOperator) child).operand)).accept(this);
        } else if (child instanceof Disjunction) {
            ArrayList<Formula> newChildren = ((PropositionalFormula) child).children.stream().map(FOperator::new)
                    .collect(Collectors.toCollection(ArrayList::new));
            return Simplifier.simplify(new Disjunction(newChildren), Simplifier.Strategy.PROPOSITIONAL).accept(this);
        } else {
            return new FOperator(child);
        }
    }

    @Override
    public Formula visit(GOperator g) {
        Formula child = g.operand.accept(this);
        if (child instanceof BooleanConstant || child instanceof GOperator) {
            return child;
        } else if (child instanceof XOperator) {
            return new XOperator(new GOperator(((ModalOperator) child).operand)).accept(this);
        }
        return new GOperator(child);
    }

    @Override
    public Formula visit(UOperator u) {
        if (u.right.isSuspendable() || u.right.isPureEventual()) {
            return u.right.accept(this);
        } else {
            Formula l = u.left.accept(this);
            Formula r = u.right.accept(this);
            ImplicationVisitor imp = ImplicationVisitor.getVisitor();
            if (l.accept(imp, r) || r instanceof BooleanConstant) {
                return r;
            } else if (l instanceof BooleanConstant) {
                if (((BooleanConstant) l).value) {
                    return new FOperator(r).accept(this);
                } else {
                    return r;
                }
            } else if (r instanceof FOperator) {
                return r;
            } else if (l instanceof FOperator) {
                return Simplifier.simplify(new Disjunction(r, new FOperator(Simplifier.simplify(new Conjunction(new XOperator(r), l), Simplifier.Strategy.PROPOSITIONAL))), Simplifier.Strategy.PROPOSITIONAL)
                        .accept(this);
            } else if (l.accept(imp, r)) {
                return r;
            } else if (l instanceof GOperator) {
                return Simplifier.simplify(new Disjunction(Simplifier.simplify(new Conjunction(l, new FOperator(r)), Simplifier.Strategy.PROPOSITIONAL), r), Simplifier.Strategy.PROPOSITIONAL).accept(this);
            } else if (l instanceof XOperator && r instanceof XOperator) {
                return new XOperator(new UOperator(((ModalOperator) l).operand, ((ModalOperator) r).operand));
            }
            return new UOperator(l, r);
        }
    }

    @Override
    public Formula visit(XOperator x) {
        Formula child = x.operand.accept(this);
        if (child.isSuspendable()) {
            return child;
        } else if (child.equals(x.operand)) {
            return x;
        } else if (child instanceof BooleanConstant) {
            return child;
        } else {
            return new XOperator(child);
        }
    }
}
