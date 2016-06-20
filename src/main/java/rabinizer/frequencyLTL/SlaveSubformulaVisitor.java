package rabinizer.frequencyLTL;

import java.util.HashSet;
import java.util.Set;

import ltl.BooleanConstant;
import ltl.Conjunction;
import ltl.Disjunction;
import ltl.FOperator;
import ltl.Formula;
import ltl.GOperator;
import ltl.Literal;
import ltl.UOperator;
import ltl.Visitor;
import ltl.XOperator;

public class SlaveSubformulaVisitor implements Visitor<Set<Formula>> {

    @Override
    public Set<Formula> defaultAction(Formula formula) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Formula> visit(BooleanConstant b) {
        return new HashSet<>();
    }

    @Override
    public Set<Formula> visit(Conjunction conjunction) {
        return conjunction.union(f -> f.accept(this));
    }

    @Override
    public Set<Formula> visit(Disjunction disjunction) {
        return disjunction.union(f -> f.accept(this));
    }

    @Override
    public Set<Formula> visit(FOperator fOperator) {
        Set<Formula> result = fOperator.operand.accept(this);
        if (fOperator instanceof FOperatorForMojmir) {
            result.add(fOperator);
        }
        return result;
    }

    @Override
    public Set<Formula> visit(GOperator gOperator) {
        Set<Formula> result = gOperator.operand.accept(this);
        result.add(gOperator);
        return result;
    }

    @Override
    public Set<Formula> visit(Literal literal) {
        return new HashSet<>();
    }

    @Override
    public Set<Formula> visit(UOperator uOperator) {
        Set<Formula> result = uOperator.left.accept(this);
        result.addAll(uOperator.right.accept(this));
        return result;
    }

    @Override
    public Set<Formula> visit(XOperator xOperator) {
        return xOperator.operand.accept(this);
    }
}
