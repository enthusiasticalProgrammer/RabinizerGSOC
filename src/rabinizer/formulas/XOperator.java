package rabinizer.formulas;

import java.util.ArrayList;
import com.microsoft.z3.*;


public class XOperator extends FormulaUnary {

    @Override
    public String operator() {
        return "X";
    }

    public XOperator(Formula f) {
        super(f);
    }

    @Override
    public XOperator ThisTypeUnary(Formula operand) {
        return new XOperator(operand);
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
        return new XOperator(operand.toNNF());
    }

    @Override
    public Formula negationToNNF() {
        return new XOperator(operand.negationToNNF());
    }

    //============== OVERRIDE ====================
    @Override
    public Formula removeX() {
        return operand;
    }
    
    public BoolExpr toExpr(Context ctx){
   		return ctx.mkBoolConst(toZ3String(true));
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
		return new XOperator(child);
	}

	@Override
	public Formula simplifyLocally() {
		Formula child=operand.simplifyLocally();
		if(child instanceof BooleanConstant){
			return child;
		}else{
			return new XOperator(child);
		}
	}

}
