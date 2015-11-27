package rabinizer.formulas;

public interface FormulaVisitor {
    Formula visitB(BooleanConstant b);

    Formula visitC(Conjunction c);

    Formula visitD(Disjunction b);

    Formula visitF(FOperator f);

    Formula visitG(GOperator g);

    Formula visitL(Literal l);

    Formula visitN(Negation n);

    Formula visitU(UOperator u);

    Formula visitX(XOperator x);
}
