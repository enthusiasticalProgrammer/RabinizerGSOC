package rabinizer.ltl;

import java.util.HashSet;
import java.util.Set;

public class ContainsVisitor implements Visitor<Boolean> {
    private final Class c;

    public ContainsVisitor(Class cl) {
        if (!Formula.class.isAssignableFrom(cl)) {
            throw new IllegalArgumentException("");
        }
        this.c = cl;
    }

    @Override
    public Boolean defaultAction(Formula formula) {
        throw new AssertionError();
    }

    @Override
    public Boolean visit(BooleanConstant booleanConstant) {
        return c.equals(BooleanConstant.class);
    }

    @Override
    public Boolean visit(Conjunction conjunction) {
        if (c.equals(Conjunction.class)) {
            return true;
        }
        return conjunction.children.stream().anyMatch(child -> child.accept(this));
    }

    @Override
    public Boolean visit(Disjunction disjunction) {
        if (c.equals(Disjunction.class)) {
            return true;
        }
        return disjunction.children.stream().anyMatch(child -> child.accept(this));
    }

    @Override
    public Boolean visit(FOperator fOperator) {
        if (c.equals(FOperator.class)) {
            return true;
        }
        return fOperator.operand.accept(this);
    }

    @Override
    public Boolean visit(GOperator gOperator) {
        if (c.equals(GOperator.class)) {
            return true;
        }
        return gOperator.operand.accept(this);
    }

    @Override
    public Boolean visit(Literal literal) {
        return c.equals(Literal.class);
    }

    @Override
    public Boolean visit(UOperator uOperator) {

        if (c.equals(UOperator.class)) {
            return true;
        }
        return uOperator.left.accept(this) || uOperator.right.accept(this);
    }

    @Override
    public Boolean visit(XOperator xOperator) {
        if (c.equals(XOperator.class)) {
            return true;
        }
        return xOperator.operand.accept(this);
    }

}
