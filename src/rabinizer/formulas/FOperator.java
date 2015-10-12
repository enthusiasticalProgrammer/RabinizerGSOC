package rabinizer.formulas;
import java.util.ArrayList;

import com.microsoft.z3.*;

public class FOperator extends FormulaUnary {

    @Override
    public String operator() {
        return "F";
    }

    public FOperator(Formula f) {
        super(f);
    }

    @Override
    public FOperator ThisTypeUnary(Formula operand) {
        return new FOperator(operand);
    }

    @Override
    public Formula unfold() {
        // U(F phi) = U(phi) \/ X F U(phi)
        return new Disjunction(operand.unfold(), /*new XOperator*/ (this));
    }

    @Override
    public Formula unfoldNoG() {
        // U(F phi) = U(phi) \/ X F U(phi)
        return new Disjunction(operand.unfoldNoG(), /*new XOperator*/ (this));
    }

    @Override
    public Formula toNNF() {
        return new FOperator(operand.toNNF());
    }

    @Override
    public Formula negationToNNF() {
        return new GOperator(operand.negationToNNF());
    }

    public BoolExpr toExpr(Context ctx){
    	if(operand.toExpr(ctx).isTrue()){
    		return ctx.mkTrue();
    	}else if(operand.toExpr(ctx).isFalse()){
    		return ctx.mkFalse();
    	}
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
			return "F"+child;
		}
	}

	@Override
	public ArrayList<String> getAllPropositions() {
		ArrayList<String> a=new ArrayList<String>();
		a.add(toZ3String(true));
		return a;
	}
}
