package rabinizer.frequencyLTL;

import java.util.Set;

import ltl.BinaryVisitor;
import ltl.BooleanConstant;
import ltl.Conjunction;
import ltl.Disjunction;
import ltl.FOperator;
import ltl.GOperator;
import ltl.Literal;
import ltl.UOperator;
import ltl.XOperator;
import ltl.Formula;

public class EvaluationVisitor implements BinaryVisitor<Formula, Set<Formula>> {
    @Override
    public Formula visit(BooleanConstant b, Set<Formula> Gs) {
        return b;
    }

    @Override
    public Formula visit(Conjunction c, Set<Formula> Gs) {
        return Conjunction.create(c.children.stream().map(child -> child.accept(this, Gs)));
    }


    @Override
    public Formula visit(Disjunction d, Set<Formula> Gs) {
        return Disjunction.create(d.children.stream().map(child -> child.accept(this, Gs)));
    }

    @Override
    public Formula visit(FOperator f, Set<Formula> Gs) {
        if (f instanceof FOperatorForMojmir) {
            return BooleanConstant.get(Gs.contains(f));
        }
        Formula op = f.operand.accept(this, Gs);
        if (!op.equals(f.operand)) {
            return FOperator.create(op);
        }
        return f;
    }

    @Override
    public Formula visit(GOperator g, Set<Formula> Gs) {
        return BooleanConstant.get(Gs.contains(g));
    }

    @Override
    public Formula visit(Literal l, Set<Formula> Gs) {
        return l;
    }

    @Override
    public Formula visit(UOperator u, Set<Formula> Gs) {
        return UOperator.create(u.left.accept(this, Gs), u.right.accept(this, Gs));
    }

    @Override
    public Formula visit(XOperator x, Set<Formula> Gs) {
        Formula operand = x.operand.accept(this, Gs);

        if (operand == x.operand) {
            return x;
        }

        return XOperator.create(operand.accept(this, Gs));
    }
}
