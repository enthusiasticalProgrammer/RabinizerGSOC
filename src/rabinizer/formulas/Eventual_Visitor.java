package rabinizer.formulas;

//visit returns true if formula is pure_eventual
public class Eventual_Visitor implements Attribute_Visitor{

	private static Eventual_Visitor instance=new Eventual_Visitor();
	
	//to overwrite the public default constructor-->other classes have to call getVisitor()
	private Eventual_Visitor(){
		super();
	}
	
	public static Eventual_Visitor getVisitor(){
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
	public boolean visitG(GOperator g) {
		return g.operand.acceptBool(this);
	}

	@Override
	public boolean visitL(Literal l) {
		return false;
	}

	@Override
	public boolean visitN(Negation n) {
		return n.operand.acceptBool(Universality_Visitor.getVisitor());
	}

	@Override
	public boolean visitU(UOperator u) {
		return u.right.acceptBool(this);
	}

	@Override
	public boolean visitX(XOperator x) {
		return x.operand.acceptBool(this);
	}

	@Override
	public boolean visitF(FOperator f) {
		return true;
	}
	
	
}
