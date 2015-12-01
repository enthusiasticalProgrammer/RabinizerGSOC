package rabinizer.ltl;

public interface Visitor<R> {
    R visit(BooleanConstant b);
    R visit(Conjunction c);
    R visit(Disjunction b);
    R visit(FOperator f);
    R visit(GOperator g);
    R visit(Literal l);
    R visit(UOperator u);
    R visit(XOperator x);
}
