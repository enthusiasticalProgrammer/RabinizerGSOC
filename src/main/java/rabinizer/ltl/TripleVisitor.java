package rabinizer.ltl;

public interface TripleVisitor<A, B, C> {
    A visit(BooleanConstant bo, B b, C c);

    A visit(Conjunction co, B b, C c);

    A visit(Disjunction d, B b, C c);

    A visit(FOperator f, B b, C c);

    A visit(GOperator g, B b, C c);

    A visit(Literal l, B b, C c);

    A visit(UOperator u, B b, C c);

    A visit(XOperator x, B b, C c);
}
