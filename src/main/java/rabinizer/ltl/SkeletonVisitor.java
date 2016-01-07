package rabinizer.ltl;

public class SkeletonVisitor implements Visitor<Formula> {

    @Override
    public Formula defaultAction(Formula formula) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Formula visit(BooleanConstant booleanConstant) {
        return booleanConstant;
    }

    @Override
    public Formula visit(Conjunction conjunction) {
        return new Conjunction(conjunction.getChildren().stream().map(c -> c.accept(this)));
    }

    @Override
    public Formula visit(Disjunction disjunction) {
        return new Disjunction(disjunction.getChildren().stream().map(c -> c.accept(this)));
    }

    @Override
    public Formula visit(FOperator fOperator) {
        return fOperator.operand.accept(this);
    }

    @Override
    public Formula visit(GOperator gOperator) {
        return new Conjunction(gOperator, gOperator.operand.accept(this));
    }

    @Override
    public Formula visit(Literal literal) {
        return BooleanConstant.TRUE;
    }

    @Override
    public Formula visit(UOperator uOperator) {
        return uOperator.right.accept(this);
    }

    @Override
    public Formula visit(XOperator xOperator) {
        return xOperator.operand.accept(this);
    }
}
