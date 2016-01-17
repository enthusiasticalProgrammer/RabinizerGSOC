package rabinizer.ltl;

import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class GSubstitutionVisitor implements Visitor<Formula> {

    private final Function<GOperator, BooleanConstant> subst;

    public GSubstitutionVisitor(Function<GOperator, BooleanConstant> subst) {
        this.subst = subst;
    }

    @Override
    public Formula defaultAction(@NotNull Formula f) {
        return f;
    }

    @Override
    public Formula visit(@NotNull Conjunction c) {
        return new Conjunction(c.children.stream().map(e -> e.accept(this)));
    }

    @Override
    public Formula visit(@NotNull Disjunction d) {
        return new Disjunction(d.children.stream().map(e -> e.accept(this)));
    }

    @Override
    public Formula visit(@NotNull FOperator f) {
        return new FOperator(f.operand.accept(this));
    }

    @Override
    public Formula visit(@NotNull GOperator g) {
        BooleanConstant substitutedFormula = subst.apply(g);

        if (substitutedFormula != null) {
            return substitutedFormula;
        }

        return new GOperator(g.operand.accept(this));
    }

    @Override
    public Formula visit(@NotNull UOperator u) {
        return new UOperator(u.left.accept(this), u.right.accept(this));
    }

    @Override
    public Formula visit(@NotNull XOperator x) {
        return new XOperator(x.operand.accept(this));
    }
}
