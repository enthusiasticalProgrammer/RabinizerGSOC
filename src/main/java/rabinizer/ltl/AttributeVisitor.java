package rabinizer.ltl;

public interface AttributeVisitor {
    boolean visitB(BooleanConstant b);

    boolean visitC(Conjunction c);

    boolean visitD(Disjunction b);

    boolean visitF(FOperator f);

    boolean visitG(GOperator g);

    boolean visitL(Literal l);

    boolean visitN(Negation n);

    boolean visitU(UOperator u);

    boolean visitX(XOperator x);
}
