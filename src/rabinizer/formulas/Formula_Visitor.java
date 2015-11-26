package rabinizer.formulas;

public interface Formula_Visitor {
	public abstract Formula visitB(BooleanConstant b);
	public abstract Formula visitC(Conjunction c);
	public abstract Formula visitD(Disjunction b);
	public abstract Formula visitF(FOperator f);
	public abstract Formula visitG(GOperator g);
	public abstract Formula visitL(Literal l);
	public abstract Formula visitN(Negation n);
	public abstract Formula visitU(UOperator u);
	public abstract Formula visitX(XOperator x);
}
