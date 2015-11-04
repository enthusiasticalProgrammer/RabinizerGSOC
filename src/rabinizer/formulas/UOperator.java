package rabinizer.formulas;

import java.util.*;
import com.microsoft.z3.*;
import net.sf.javabdd.*;
import rabinizer.bdd.BDDForFormulae;
import rabinizer.bdd.Valuation;

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


    public BDD bdd() {
        if (cachedBdd == null) {
            Formula booleanAtom = FormulaFactory.mkU(
                left.representative(),
                right.representative()
            );
            int bddVar = BDDForFormulae.bijectionBooleanAtomBddVar.id(booleanAtom);
            if (BDDForFormulae.bddFactory.varNum() <= bddVar) {
                BDDForFormulae.bddFactory.extVarNum(bddVar);
            }
            cachedBdd = BDDForFormulae.bddFactory.ithVar(bddVar);
            BDDForFormulae.representativeOfBdd(cachedBdd, this);
        }
        return cachedBdd;
    } 
       
    UOperator(Formula left, Formula right,long id) {
        super(left, right,id);
    }

    @Override
    public Formula unfold() {
        // unfold(a U b) = unfold(b) v (unfold(a) ^ X (a U b))
        return FormulaFactory.mkOr(right.unfold(),FormulaFactory.mkAnd(left.unfold(), /*new XOperator*/ (this)));
    }

    @Override
    public Formula unfoldNoG() {
        // unfold(a U b) = unfold(b) v (unfold(a) ^ X (a U b))
        return FormulaFactory.mkOr(right.unfoldNoG(),FormulaFactory.mkAnd(left.unfoldNoG(), /*new XOperator*/ (this)));
    }

    public Formula toNNF() {
        return FormulaFactory.mkU(left.toNNF(), right.toNNF());
    }

    public Formula negationToNNF() {
        return FormulaFactory.mkOr(FormulaFactory.mkG(right.negationToNNF()),
        		FormulaFactory.mkU(right.negationToNNF(), FormulaFactory.mkAnd(
        				left.negationToNNF(), right.negationToNNF())));
    }
    
    public BoolExpr toExpr(Context ctx){
    	if(cachedLTL==null){
    		cachedLTL=ctx.mkBoolConst(toZ3String(true));
    	}
    	return cachedLTL;
    }
    
    @Override
    public int hashCode(){
    	return ((left.hashCode() % 33767)*(right.hashCode() % 33049))% 999983;
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

	@Override
	public Formula rmAllConstants() {
		Formula l=left.rmAllConstants();
		Formula r=right.rmAllConstants();
		if(l instanceof BooleanConstant){
			if(((BooleanConstant) l).value){
				return FormulaFactory.mkF(r);
			}else{
				return r;
			}
		}
		
		if(r instanceof BooleanConstant){
			return r;
		}
		return FormulaFactory.mkU(l,r);
	}

	@Override
	public Formula simplifyLocally() {
		Formula l=left.simplifyLocally();
		Formula r=right.simplifyLocally();
		
		if(r instanceof BooleanConstant){
			return r;
		}else if(l instanceof BooleanConstant){
			if(((BooleanConstant) l).value){
				return FormulaFactory.mkF(r);
			}else{
				return r;
			}
		}else if(r instanceof FOperator){
			return r;
		}else if(l instanceof FOperator){
			return FormulaFactory.mkOr(r,FormulaFactory.mkF(FormulaFactory.mkAnd(FormulaFactory.mkX(r),l)));
		}else if(l instanceof Literal && r instanceof Literal){
			if(((Literal) l).atom.equals(((Literal) r).atom)){
				if((((Literal) l).negated)==(((Literal) r).negated)){
					return FormulaFactory.mkConst(true);
				}
			}
		}else if(l instanceof GOperator){
			return FormulaFactory.mkOr(FormulaFactory.mkAnd(l,FormulaFactory.mkF(r)),r);
		}else if(l instanceof XOperator && r instanceof XOperator){
			return FormulaFactory.mkX(FormulaFactory.mkU(((XOperator) l).operand,((XOperator) r).operand));
		}
		
		if(l==left && r==right){
			return this;
		}
		return FormulaFactory.mkU(l,r);
		
	}

	@Override
	public Formula setToConst(long id, boolean constant) {
		if(id==unique_id){
			return FormulaFactory.mkConst(constant);
		}else{
			return this;
		}
			
	}
	

}
