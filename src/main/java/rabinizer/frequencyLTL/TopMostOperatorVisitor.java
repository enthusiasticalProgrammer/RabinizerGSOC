package rabinizer.frequencyLTL;

import java.util.HashSet;
import java.util.Set;

import ltl.*;
import ltl.visitors.Visitor;

public class TopMostOperatorVisitor implements Visitor<Set<UnaryModalOperator>> {

    @Override
    public Set<UnaryModalOperator> defaultAction(Formula formula) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<UnaryModalOperator> visit(BooleanConstant b) {
        return new HashSet<>();
    }

    @Override
    public Set<UnaryModalOperator> visit(Conjunction conjunction) {
        return conjunction.union(f -> f.accept(this));
    }

    @Override
    public Set<UnaryModalOperator> visit(Disjunction disjunction) {
        return disjunction.union(f -> f.accept(this));
    }

    @Override
    public Set<UnaryModalOperator> visit(FOperator fOperator) {
        Set<UnaryModalOperator> result = new HashSet<>();
        if (fOperator instanceof FOperatorForMojmir) {
            result.add(fOperator);
        } else {
            result.addAll(fOperator.operand.accept(this));
        }
        return result;
    }

    @Override
    public Set<UnaryModalOperator> visit(GOperator gOperator) {
        Set<UnaryModalOperator> result = new HashSet<>();
        result.add(gOperator);
        return result;
    }

    @Override
    public Set<UnaryModalOperator> visit(Literal literal) {
        return new HashSet<>();
    }

    @Override
    public Set<UnaryModalOperator> visit(UOperator uOperator) {
        Set<UnaryModalOperator> result = uOperator.left.accept(this);
        result.addAll(uOperator.right.accept(this));
        return result;
    }

    @Override
    public Set<UnaryModalOperator> visit(XOperator xOperator) {
        return xOperator.operand.accept(this);
    }
}