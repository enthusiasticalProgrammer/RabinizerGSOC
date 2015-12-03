package rabinizer.ltl;

public interface BinaryVisitor<A, B> {
    A visit(BooleanConstant b, B fo);

    A visit(Conjunction c, B fo);

    A visit(Disjunction b, B fo);

    A visit(FOperator f, B fo);

    A visit(GOperator g, B fo);

    A visit(Literal l, B fo);

    A visit(UOperator u, B fo);

    A visit(XOperator x, B fo);
}
