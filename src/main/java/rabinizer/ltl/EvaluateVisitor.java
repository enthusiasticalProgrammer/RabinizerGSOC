package rabinizer.ltl;

import java.util.Set;

public class EvaluateVisitor implements Visitor<Formula> {

    private final EquivalenceClassFactory factory;
    private final EquivalenceClass environment;

    public EvaluateVisitor(EquivalenceClassFactory factory, Formula environment) {
        this.factory = factory;
        this.environment = factory.createEquivalenceClass(environment);
    }

    public EvaluateVisitor(EquivalenceClassFactory factory, Set<Formula> environment) {
        this(factory, new Conjunction(environment));
    }

    @Override
    public Formula defaultAction(Formula f) {
        if (environment.implies(factory.createEquivalenceClass(f))) {
            return BooleanConstant.TRUE;
        }

        return f;
    }

    @Override
    public Formula visit(Conjunction c) {
        return new Conjunction(c.children.stream().map(e -> e.accept(this)));
    }

    @Override
    public Formula visit(Disjunction d) {
        Formula defaultAction = defaultAction(d);

        if (defaultAction instanceof BooleanConstant) {
            return defaultAction;
        }

        return new Disjunction(d.children.stream().map(e -> e.accept(this)));
    }
}
