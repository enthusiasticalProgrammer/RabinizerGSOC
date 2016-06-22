package rabinizer.frequencyLTL;

import ltl.Conjunction;
import ltl.Disjunction;
import ltl.FOperator;
import ltl.Formula;
import ltl.GOperator;
import ltl.UOperator;
import ltl.Visitor;
import ltl.XOperator;

/**
 * This Visitor visits a formula and it replaces 'ordinary' F-operators by
 * FOperatorForMojmir. The motivation is that for FOperatorForMojmir, we make a
 * Mojmir-automaton and for ordinary FOperators not.
 */
public class MojmirOperatorVisitor implements Visitor<Formula> {

    @Override
    public Formula defaultAction(Formula formula) {
        return formula;
    }

    @Override
    public Formula visit(Conjunction c) {
        return Conjunction.create(c.children.stream().map(child -> child.accept(this)));
    }

    @Override
    public Formula visit(Disjunction d) {
        return Disjunction.create(d.children.stream().map(child -> child.accept(this)));
    }

    @Override
    public FOperatorForMojmir visit(FOperator fOperator) {
        return new FOperatorForMojmir(fOperator.operand.accept(this));
    }

    @Override
    public Formula visit(GOperator gOperator) {
        Formula formula = gOperator.operand.accept(this);
        if (!formula.equals(gOperator.operand)) {
            return new GOperator(formula);
        }
        return gOperator;
    }

    @Override
    public Formula visit(UOperator uOperator) {
        Formula left = uOperator.left.accept(this);
        Formula right = uOperator.right.accept(this);
        if (!left.equals(uOperator.left) || !right.equals(uOperator.right)) {
            return new UOperator(left, right);
        }
        return uOperator;
    }

    @Override
    public Formula visit(XOperator xOperator) {
        Formula formula = xOperator.operand.accept(this);
        if (!formula.equals(xOperator.operand)) {
            return new XOperator(formula);
        }
        return xOperator;
    }
}
