package rabinizer.automata.output;

import jhoafparser.ast.AtomLabel;
import jhoafparser.ast.BooleanExpression;
import org.jetbrains.annotations.Contract;
import rabinizer.ltl.*;

public class FormulaConverter implements rabinizer.ltl.Visitor<BooleanExpression<AtomLabel>> {

    public static final FormulaConverter converter = new FormulaConverter();

    private static final BooleanExpression<AtomLabel> TRUE = new BooleanExpression<AtomLabel>(true);
    private static final BooleanExpression<AtomLabel> FALSE = new BooleanExpression<AtomLabel>(false);

    @Contract(pure = true)
    private static BooleanExpression<AtomLabel> getConstant(boolean b) {
        return b ? TRUE : FALSE;
    }

    @Override
    public BooleanExpression<AtomLabel> defaultAction(Formula f) {
        throw new IllegalArgumentException("Cannot convert " + f + " to BooleanExpression.");
    }

    @Override
    public BooleanExpression<AtomLabel> visit(BooleanConstant b) {
        return getConstant(b.value);
    }

    @Override
    public BooleanExpression<AtomLabel> visit(Conjunction c) {
        return c.children.stream().map(e -> e.accept(this)).reduce(getConstant(true), (e1, e2) -> e1.and(e2));
    }

    @Override
    public BooleanExpression<AtomLabel> visit(Disjunction d) {
        return d.children.stream().map(e -> e.accept(this)).reduce(getConstant(false), (e1, e2) -> e1.or(e2));
    }

    @Override
    public BooleanExpression<AtomLabel> visit(Literal l) {
        BooleanExpression<AtomLabel> atom = new BooleanExpression<>(AtomLabel.createAlias(l.getAtom()));

        if (l.getNegated()) {
            atom = atom.not();
        }

        return atom;
    }
}
