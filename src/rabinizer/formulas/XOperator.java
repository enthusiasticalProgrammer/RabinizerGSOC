package rabinizer.formulas;

import java.util.ArrayList;
import com.microsoft.z3.*;


public class XOperator extends FormulaUnary {

	private final int cachedHash;
	

    public String operator() {
        return "X";
    }

    XOperator(Formula f,long id) {
        super(f,id);
        this.cachedHash = init_hash();
    }

    @Override
    public Formula ThisTypeUnary(Formula operand) {
        return FormulaFactory.mkX(operand);
    }

    @Override
    public Formula unfold() {
        return this;
    }

    @Override
    public Formula unfoldNoG() {
        return this;
    }

    @Override
    public Formula toNNF() {
        return FormulaFactory.mkX(operand.toNNF());
    }

    @Override
    public Formula negationToNNF() {
        return FormulaFactory.mkX(operand.negationToNNF());
    }

    //============== OVERRIDE ====================
    @Override
    public Formula removeX() {
        return operand;
    }
    
    public BoolExpr toExpr(Context ctx){
    	if(cachedLTL==null){
    		cachedLTL=ctx.mkBoolConst(toZ3String(true));
    	}
   		return cachedLTL;
    }

    @Override
    public int hashCode(){
    	return cachedHash;
    }
    
	@Override
	public String toZ3String(boolean is_atom) {
		
		return "X"+operand.toZ3String(true);
	}

	@Override
	public ArrayList<String> getAllPropositions() {
		ArrayList<String> a=new ArrayList<String>();
		a.add(toZ3String(true));
		return a;
	}

	@Override
	public Formula rmAllConstants() {
		Formula child=operand.rmAllConstants();
		if(child instanceof BooleanConstant){
			return child;
		}
		return FormulaFactory.mkX(child);
	}

	private int init_hash() {
		return (((operand.hashCode() % 33791) * 32687) + 701) % 999983;
	}

	@Override
	public Formula acceptFormula(Formula_Visitor v) {
		return v.visitX(this);
	}

	@Override
	public boolean acceptBool(Attribute_Visitor v) {
		return v.visitX(this);
	}

	@Override
	public boolean acceptBinarybool(Attribute_Binary_Visitor v, Formula f) {
		return v.visitX(this, f);
	}
	
	


}
