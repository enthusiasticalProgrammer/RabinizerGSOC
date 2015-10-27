package rabinizer.formulas;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;

import rabinizer.z3.GSet;

public class GOperator extends FormulaUnary {

    @Override
    public String operator() {
        return "G";
    }

    public GOperator(Formula f) {
        super(f);
    }

    @Override
    public GOperator ThisTypeUnary(Formula operand) {
        return new GOperator(operand);
    }

    @Override
    public Formula unfold() {
        // U(F phi) = U(phi) \/ X F U(phi)
        return new Conjunction(operand.unfold(), /*new XOperator*/ (this));
    }

    @Override
    public Formula unfoldNoG() {
        return this;
    }

    @Override
    public Formula toNNF() {
        return new GOperator(operand.toNNF());
    }

    @Override
    public Formula negationToNNF() {
        return new FOperator(operand.negationToNNF());
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
            return new BooleanConstant(false);
        } else {
            return this;
        }
    }
    
    
    public BoolExpr toExpr(Context ctx){
    	return ctx.mkBoolConst(toZ3String(true));
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
		return new GOperator(child);
	}

	@Override
	public Formula simplifyLocally() {
		Formula child=operand.simplifyLocally();
		if(child instanceof BooleanConstant){
			return child;
		}else if(child instanceof GOperator){
			return child;
		}else if(child instanceof UOperator){
			return new GOperator(new Conjunction(new FOperator(((UOperator) child).right),new Disjunction(((UOperator) child).left,((UOperator) child).right)));
		}else if(child instanceof XOperator){
			return new XOperator(new GOperator(((XOperator) child).operand));
		}
		else return new GOperator(child);
	}

}
