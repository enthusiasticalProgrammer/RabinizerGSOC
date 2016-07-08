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
import ltl.ModalOperator;
import ltl.UOperator;
import ltl.visitors.Visitor;
import ltl.XOperator;

public class TopMostOperatorVisitor implements Visitor<Set<ModalOperator>> {

    @Override
    public Set<ModalOperator> defaultAction(Formula formula) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<ModalOperator> visit(BooleanConstant b) {
        return new HashSet<>();
    }

    @Override
    public Set<ModalOperator> visit(Conjunction conjunction) {
        return conjunction.union(f -> f.accept(this));
    }

    @Override
    public Set<ModalOperator> visit(Disjunction disjunction) {
        return disjunction.union(f -> f.accept(this));
    }

    @Override
    public Set<ModalOperator> visit(FOperator fOperator) {
        Set<ModalOperator> result = new HashSet<>();
        if (fOperator instanceof FOperatorForMojmir) {
            result.add(fOperator);
        } else {
            result.addAll(fOperator.operand.accept(this));
        }
        return result;
    }

    @Override
    public Set<ModalOperator> visit(GOperator gOperator) {
        Set<ModalOperator> result = new HashSet<>();
        result.add(gOperator);
        return result;
    }

    @Override
    public Set<ModalOperator> visit(Literal literal) {
        return new HashSet<>();
    }

    @Override
    public Set<ModalOperator> visit(UOperator uOperator) {
        Set<ModalOperator> result = uOperator.left.accept(this);
        result.addAll(uOperator.right.accept(this));
        return result;
    }

    @Override
    public Set<ModalOperator> visit(XOperator xOperator) {
        return xOperator.operand.accept(this);
    }
}