package rabinizer.formulas;

import java.util.ArrayList;
import java.util.Arrays;

import com.microsoft.z3.*;
import net.sf.javabdd.BDD;
import rabinizer.bdd.BDDForFormulae;

public class Negation extends FormulaUnary {


	private final int cachedHash;
	
    @Override
    public String operator() {
        return "!";
    }

    Negation(Formula f,long id) {
        super(f,id);
        this.cachedHash = init_hash(); 
    }

    public Formula ThisTypeUnary(Formula operand) {
        return FormulaFactory.mkNot(operand);
    }

    public BDD bdd() {            // negation of ATOMIC PROPOSITIONS only
        if (cachedBdd == null) {
            Formula booleanAtom = FormulaFactory.mkNot(operand.representative());
            int bddVar = BDDForFormulae.bijectionBooleanAtomBddVar.id(booleanAtom);
            if (BDDForFormulae.bddFactory.varNum() <= bddVar) {
                BDDForFormulae.bddFactory.extVarNum(1);
            }
            cachedBdd = BDDForFormulae.bddFactory.ithVar(bddVar);
            BDDForFormulae.representativeOfBdd(cachedBdd, this);
        }
        return cachedBdd;
    }
    
 

    @Override
    public Formula unfold() {
        throw new UnsupportedOperationException("Supported for NNF only.");
    }

    @Override
    public Formula unfoldNoG() {
        throw new UnsupportedOperationException("Supported for NNF only.");
    }
 

    
    public BoolExpr toExpr(Context ctx){
      	if(cachedLTL==null){
      		cachedLTL=ctx.mkNot(operand.toExpr(ctx));
      	}
    	return cachedLTL;
    }
    

    @Override
    public Formula toNNF() {
        return operand.negationToNNF();
    }

    @Override
    public Formula negationToNNF() {
        return operand.toNNF();
    }

    @Override
    public int hashCode(){
    	return cachedHash;
    }
    
	@Override
	public String toZ3String(boolean is_atom) {
		String child=operand.toZ3String(is_atom);
		if(child.equals("true")){
			return "false";
		}else if(child.equals("false")){
			return "true";
		}else if(is_atom){
			return "!"+child;
		}else{
			return "(not "+child+" )";
		}
	}

	@Override
	public ArrayList<String> getAllPropositions() {
		String child=operand.toZ3String(true);
		ArrayList<String> a=new ArrayList<String>();
		if(!child.equals("true")&& !child.equals("false")){
			a.addAll(operand.getAllPropositions());
		}
		return a;
	}

	@Override
	public Formula rmAllConstants() {
		Formula child=operand.rmAllConstants();
		if(child instanceof BooleanConstant){
			return FormulaFactory.mkConst(!((BooleanConstant) child).get_value());
		}
		return FormulaFactory.mkNot(child);
	}

	private int init_hash() {
		return (((operand.hashCode() % 38867) * 33317) + 3449) % 999983;
	}

	@Override
	public Formula acceptFormula(Formula_Visitor v) {
		return v.visitN(this);
	}

	@Override
	public boolean acceptBool(Attribute_Visitor v) {
		return v.visitN(this);
	}

	@Override
	public boolean acceptBinarybool(Attribute_Binary_Visitor v, Formula f) {
		return v.visitN(this, f);
	}
}
