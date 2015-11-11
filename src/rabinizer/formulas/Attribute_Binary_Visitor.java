package rabinizer.formulas;

public interface Attribute_Binary_Visitor {
	public abstract boolean visitB(BooleanConstant b, Formula fo);
	public abstract boolean visitC(Conjunction c, Formula fo);
	public abstract boolean visitD(Disjunction b, Formula fo);
	public abstract boolean visitF(FOperator f, Formula fo);
	public abstract boolean visitG(GOperator g, Formula fo);
	public abstract boolean visitL(Literal l, Formula fo);
	public abstract boolean visitN(Negation n, Formula fo);
	public abstract boolean visitU(UOperator u, Formula fo);
	public abstract boolean visitX(XOperator x, Formula fo);
}
