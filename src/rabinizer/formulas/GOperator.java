package rabinizer.formulas;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;


import rabinizer.bdd.GSet;
import rabinizer.bdd.Valuation;

public class GOperator extends FormulaUnary {

    @Override
    public String operator() {
        return "G";
    }

    GOperator(Formula f,long id) {
        super(f,id);
    }

    @Override
    public Formula ThisTypeUnary(Formula operand) {
        return FormulaFactory.mkG(operand);
    }

    @Override
    public Formula unfold() {
        // U(F phi) = U(phi) \/ X F U(phi)
        return FormulaFactory.mkAnd(operand.unfold(), /*new XOperator*/ (this));
    }

    @Override
    public Formula unfoldNoG() {
        return this;
    }

    @Override
    public Formula toNNF() {
        return FormulaFactory.mkG(operand.toNNF());
    }

    @Override
    public Formula negationToNNF() {
        return FormulaFactory.mkF(operand.negationToNNF());
    }

    //============== OVERRIDE ====================
    @Override
    public boolean containsG() {
        return true;
    }

    @Override
    public Set<Formula> gSubformulas() {
        Set<Formula> r = operand.gSubformulas();
        r.add(operand);
        return r;
    }

    @Override
    public Set<Formula> topmostGs() {
        Set<Formula> result = new HashSet<Formula>();
        result.add(this.operand);
        return result;
    }

    @Override
    public Formula substituteGsToFalse(GSet gSet) {
        if (gSet.contains(operand)) {
            return FormulaFactory.mkConst(false);
        } else {
            return this;
        }
    }
    
    
    public BoolExpr toExpr(Context ctx){
    	if(cachedLTL==null){
    		cachedLTL = ctx.mkBoolConst(toZ3String(true));
    	}
    	return cachedLTL;
    }

    @Override
    public int hashCode(){
    	return ((operand.hashCode() % 35023) *31277) % 999983;
    }
    
	@Override
	public String toZ3String(boolean is_atom) {
		
		String child=operand.toZ3String(true);
		if(child.equals("true")){
			return "true";
		}else if(child.equals("false")){
			return "false";
		}else{
			return "G"+child;
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
		return FormulaFactory.mkG(child);
	}

	@Override
	public Formula simplifyLocally() {
		Formula child=operand.simplifyLocally();
		if(child instanceof BooleanConstant){
			return child;
		}else if(child instanceof GOperator){
			return child;
		}else if(child instanceof XOperator){
			return FormulaFactory.mkX(FormulaFactory.mkG(((XOperator) child).operand));
		}else {
			if(child==operand){
				return this;
			}else{
				return FormulaFactory.mkG(child);
			}
		}
	}
	
	@Override
	public Formula setToConst(long id,boolean constant){
		if(id==unique_id){
			return FormulaFactory.mkConst(constant);
		}else if(!constant){
			return FormulaFactory.mkG(operand.setToConst(id, constant)).simplifyLocally();
		}else{
			return this;
		}
	}

}
