package rabinizer.formulas;

import java.util.*;
import com.microsoft.z3.*;
import net.sf.javabdd.*;
import rabinizer.bdd.BDDForFormulae;

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

    public BDD bdd() {
        if (cachedBdd == null) {
            Formula booleanAtom = new UOperator(
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

	@Override
	public Formula rmAllConstants() {
		Formula l=left.rmAllConstants();
		Formula r=right.rmAllConstants();
		if(l instanceof BooleanConstant){
			if(((BooleanConstant) l).value){
				return new FOperator(r);
			}else{
				return r;
			}
		}
		
		if(r instanceof BooleanConstant){
			return r;
		}
		return new UOperator(l,r);
	}

	@Override
	public Formula simplifyLocally() {
		Formula l=left.simplifyLocally();
		Formula r=right.simplifyLocally();
		
		if(r instanceof BooleanConstant){
			return r;
		}else if(l instanceof BooleanConstant){
			if(((BooleanConstant) l).value){
				return new FOperator(r);
			}else{
				return r;
			}
		}else if(r instanceof FOperator){
			return r;
		}else if(l instanceof FOperator){
			return new Disjunction(r,new FOperator(new Conjunction(new XOperator(r),l)));
		}else if(l instanceof Literal && r instanceof Literal){
			if(((Literal) l).atom.equals(((Literal) r).atom)){
				if((((Literal) l).negated)==(((Literal) r).negated)){
					return new BooleanConstant(true);
				}
			}
		}else if(l instanceof GOperator){
			return new Disjunction(new Conjunction(l,new FOperator(r)),r);
		}else if(l instanceof XOperator && r instanceof XOperator){
			return new XOperator(new UOperator(((XOperator) l).operand,((XOperator) r).operand));
		}
		return new UOperator(l,r);
	}

}
