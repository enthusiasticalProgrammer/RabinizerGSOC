package rabinizer.formulas;

import java.util.*;
import com.microsoft.z3.*;

/**
 * Represents a until formula.
 *
 * @author Andreas & Ruslan
 *
 */
public class UOperator extends FormulaBinary {

    @Override
    public String operator() {
        return "U";
    }

    public UOperator(Formula left, Formula right) {
        super(left, right);
    }

 
    @Override
    public Formula unfold() {
        // unfold(a U b) = unfold(b) v (unfold(a) ^ X (a U b))
        return new Disjunction(right.unfold(), new Conjunction(left.unfold(), /*new XOperator*/ (this)));
    }

    @Override
    public Formula unfoldNoG() {
        // unfold(a U b) = unfold(b) v (unfold(a) ^ X (a U b))
        return new Disjunction(right.unfoldNoG(), new Conjunction(left.unfoldNoG(), /*new XOperator*/ (this)));
    }

    public Formula toNNF() {
        return new UOperator(left.toNNF(), right.toNNF());
    }

    public Formula negationToNNF() {
        return new Disjunction(new GOperator(right.negationToNNF()),
            new UOperator(right.negationToNNF(), new Conjunction(left.negationToNNF(), right.negationToNNF())));
    }
    
    public BoolExpr toExpr(Context ctx){
    	BoolExpr l=left.toExpr(ctx);
    	BoolExpr r=right.toExpr(ctx);
    	if(l.isTrue()){
    		return ctx.mkBoolConst("F"+right.toZ3String(true));
    	}else if(l.isFalse()){
    		return r;
    	}else if(r.isTrue()){
    		return ctx.mkTrue();
    	}else if(r.isFalse()){
    		return ctx.mkFalse();
    	}
    	
    	return ctx.mkBoolConst(toZ3String(true));
    }

	@Override
	public String toZ3String(boolean is_atom) {
		String l=left.toZ3String(true);
		String r=right.toZ3String(true);
		if(r.equals("true")){
			return "true";
		}else if(l.equals("false")){
			return r;
		}else if(r.equals("false")){
			return "false";
		}
		else{
			return l+"U"+r;
		}
		
	}

	@Override
	public ArrayList<String> getAllPropositions() {
		ArrayList<String> a=new ArrayList<String>();
		if(!toZ3String(true).equals("true") && !toZ3String(true).equals("true"))
		a.add(toZ3String(true));
		return a;
	}

}
