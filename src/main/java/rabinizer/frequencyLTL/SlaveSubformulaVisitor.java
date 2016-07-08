package rabinizer.frequencyLTL;

import java.util.HashSet;
import java.util.Set;

import ltl.Conjunction;
import ltl.Disjunction;
import ltl.FOperator;
import ltl.Formula;
import ltl.GOperator;
import ltl.ModalOperator;
import ltl.UOperator;
import ltl.visitors.Visitor;
import ltl.XOperator;

public class SlaveSubformulaVisitor implements Visitor<Set<ModalOperator>> {

    @Override
    public Set<ModalOperator> defaultAction(Formula formula) {
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
        Set<ModalOperator> result = fOperator.operand.accept(this);
        if (fOperator instanceof FOperatorForMojmir) {
            result.add(fOperator);
        }
        return result;
    }

    @Override
    public Set<ModalOperator> visit(GOperator gOperator) {
        Set<ModalOperator> result = gOperator.operand.accept(this);
        result.add(gOperator);
        return result;
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
