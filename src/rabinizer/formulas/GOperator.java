package rabinizer.formulas;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.Sort;

import rabinizer.bdd.GSet;

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
    	Sort U = ctx.mkBoolSort();
    	FuncDecl g=ctx.mkFuncDecl("G",U,U);
    	return (BoolExpr) g.apply(operand.toExpr(ctx));
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

}
