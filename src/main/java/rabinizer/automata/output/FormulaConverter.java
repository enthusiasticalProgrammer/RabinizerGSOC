package rabinizer.automata.output;

import jhoafparser.ast.AtomLabel;
import jhoafparser.ast.BooleanExpression;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import rabinizer.ltl.*;

public class FormulaConverter implements rabinizer.ltl.Visitor<BooleanExpression<AtomLabel>> {

    private static final BooleanExpression<AtomLabel> TRUE = new BooleanExpression<>(true);
    private static final BooleanExpression<AtomLabel> FALSE = new BooleanExpression<>(false);

    @Contract(pure = true)
    private static BooleanExpression<AtomLabel> getConstant(boolean b) {
        return b ? TRUE : FALSE;
    }

    @Override
    public BooleanExpression<AtomLabel> defaultAction(@NotNull Formula f) {
        throw new IllegalArgumentException("Cannot convert " + f + " to BooleanExpression.");
    }

    @Override
    public BooleanExpression<AtomLabel> visit(@NotNull BooleanConstant b) {
        return getConstant(b.value);
    }

    @Override
    public BooleanExpression<AtomLabel> visit(@NotNull Conjunction c) {
        if (c.children.isEmpty()) {
            return TRUE;
        }

        return c.children.stream().map(e -> e.accept(this)).reduce((e1, e2) -> e1.and(e2)).get();
    }

    @Override
    public BooleanExpression<AtomLabel> visit(@NotNull Disjunction d) {
        if (d.children.isEmpty()) {
            return FALSE;
        }

        return d.children.stream().map(e -> e.accept(this)).reduce((e1, e2) -> e1.or(e2)).get();
    }

    @Override
    public BooleanExpression<AtomLabel> visit(@NotNull Literal l) {
        BooleanExpression<AtomLabel> atom = new BooleanExpression<>(AtomLabel.createAlias(l.getAtom()));

        if (l.getNegated()) {
            atom = atom.not();
        }

        return atom;
    }
}
