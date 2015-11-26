package rabinizer.formulas;

public interface Attribute_Visitor{
	public abstract boolean visitB(BooleanConstant b);
	public abstract boolean visitC(Conjunction c);
	public abstract boolean visitD(Disjunction b);
	public abstract boolean visitF(FOperator f);
	public abstract boolean visitG(GOperator g);
	public abstract boolean visitL(Literal l);
	public abstract boolean visitN(Negation n);
	public abstract boolean visitU(UOperator u);
	public abstract boolean visitX(XOperator x);
}
