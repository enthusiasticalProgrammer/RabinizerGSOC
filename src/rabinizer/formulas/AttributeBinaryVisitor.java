package rabinizer.formulas;

public interface AttributeBinaryVisitor {
    boolean visitB(BooleanConstant b, Formula fo);

    boolean visitC(Conjunction c, Formula fo);

    boolean visitD(Disjunction b, Formula fo);

    boolean visitF(FOperator f, Formula fo);

    boolean visitG(GOperator g, Formula fo);

    boolean visitL(Literal l, Formula fo);

    boolean visitN(Negation n, Formula fo);

    boolean visitU(UOperator u, Formula fo);

    boolean visitX(XOperator x, Formula fo);
}
