package rabinizer.formulas;
import java.util.ArrayList;

import com.microsoft.z3.*;

import rabinizer.bdd.Valuation;

public class FOperator extends FormulaUnary {

	private final int cachedHash;
	
    @Override
    public String operator() {
        return "F";
    }

    FOperator(Formula f,long id) {
        super(f,id);
        this.cachedHash = init_hash();
    }

    @Override
    public Formula ThisTypeUnary(Formula operand) {
        return FormulaFactory.mkF(operand);
    }

    @Override
    public Formula unfold() {
        // U(F phi) = U(phi) \/ X F U(phi)
        return FormulaFactory.mkOr(operand.unfold(), /*new XOperator*/ (this));
    }

    @Override
    public Formula unfoldNoG() {
        // U(F phi) = U(phi) \/ X F U(phi)
        return FormulaFactory.mkOr(operand.unfoldNoG(), /*new XOperator*/ (this));
    }

    @Override
    public Formula toNNF() {
        return FormulaFactory.mkF(operand.toNNF());
    }

    @Override
    public Formula negationToNNF() {
        return FormulaFactory.mkG(operand.negationToNNF());
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
		String child=operand.toZ3String(true);
		if(child.equals("true")){
			return "true";
		}else if(child.equals("false")){
			return "false";
		}else{
			return "F"+child;
		}
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
		return FormulaFactory.mkF(child);
	}
	
	public Formula setToConst(long id, boolean constant){
    	if(unique_id==id){
    		return FormulaFactory.mkConst(constant);
    	}else{
    		if(constant){
    			if(operand.get_id()==id){
    				return FormulaFactory.mkConst(constant);
    			}
    		}
    		return this;
    	}
    }


	
	private int init_hash() {
		return (((operand.hashCode() % 34211) * 32213) + 3499) % 999983;
	}

	@Override
	public Formula acceptFormula(Formula_Visitor v) {
		return v.visitF(this);
	}

	@Override
	public boolean acceptBool(Attribute_Visitor v) {
		return v.visitF(this);
	}

	@Override
	public boolean acceptBinarybool(Attribute_Binary_Visitor v, Formula f) {
		return v.visitF(this, f);
	}


}
