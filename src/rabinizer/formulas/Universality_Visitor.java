package rabinizer.formulas;

//returns true if the Formula is pure universal
public class Universality_Visitor implements Attribute_Visitor{
	private static Universality_Visitor instance=new Universality_Visitor();
	
	//to overwrite the public default constructor-->other classes have to call getVisitor()
	private Universality_Visitor(){
		super();
	}
	
	public static Universality_Visitor getVisitor(){
		return instance;
	}

	@Override
	public boolean visitB(BooleanConstant b) {
		return false;
	}

	@Override
	public boolean visitC(Conjunction c) {
		boolean evtl=true;
		for(Formula child: c.children){
			evtl=evtl && child.acceptBool(this);
		}
		return evtl;
	}

	@Override
	public boolean visitD(Disjunction d) {
		boolean evtl=true;
		for(Formula child: d.children){
			evtl=evtl && child.acceptBool(this);
		}
		return evtl;
	}

	@Override
	public boolean visitF(FOperator f) {
		return f.operand.acceptBool(this);
	}

	@Override
	public boolean visitG(GOperator g) {
		return true;
	}

	@Override
	public boolean visitL(Literal l) {
		return false;
	}

	@Override
	public boolean visitN(Negation n) {
		return n.operand.acceptBool(Eventual_Visitor.getVisitor());
	}

	@Override
	public boolean visitU(UOperator u) {
		return u.left.acceptBool(this) && u.right.acceptBool(this);
	}

	@Override
	public boolean visitX(XOperator x) {
		return x.operand.acceptBool(this);
	}
}
