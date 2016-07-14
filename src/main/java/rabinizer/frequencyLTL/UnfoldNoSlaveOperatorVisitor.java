package rabinizer.frequencyLTL;

import ltl.Conjunction;
import ltl.visitors.DefaultConverter;
import ltl.Disjunction;
import ltl.FOperator;
import ltl.Formula;
import ltl.GOperator;
import ltl.UOperator;
import ltl.XOperator;

public class UnfoldNoSlaveOperatorVisitor extends DefaultConverter {

    @Override
    public Formula visit(GOperator g) {
        return g;
    }

    @Override
    public Formula visit(XOperator x) {
        return x;
    }

    @Override
    public Formula visit(FOperator fOperator) {
        if (fOperator instanceof FOperatorForMojmir) {
            return fOperator;
        }
        return new Disjunction(fOperator.operand.accept(this), fOperator);
    }

    @Override
    public Formula visit(UOperator uOperator) {
        return new Disjunction(uOperator.right.accept(this), new Conjunction(uOperator.left.accept(this), uOperator));
    }
}
