package rabinizer.formulas;

import net.sf.javabdd.*;
import rabinizer.bdd.BDDForFormulae;

import java.util.ArrayList;

import com.microsoft.z3.*;

/**
 * @author Andreas Gaiser & Ruslan Ledesma-Garza
 * 	& Christopher Ziegler
 * 
 *
 */
public class Disjunction extends FormulaBinaryBoolean {

    public Disjunction(Formula left, Formula right) {
        super(left, right);
    }

    @Override
    public Disjunction ThisTypeBoolean(Formula left, Formula right) {
        return new Disjunction(left, right);
    }

    @Override
    public String operator() {
        return "|";
    }

    @Override
    public BDD bdd() {
        if (cachedBdd == null) {
            cachedBdd = left.bdd().or(right.bdd());
            BDDForFormulae.representativeOfBdd(cachedBdd, this);
        }
        return cachedBdd;
    }

    @Override
    public Formula removeConstants() {
        Formula new_left = left.removeConstants();
        if (new_left instanceof BooleanConstant) {
            if (((BooleanConstant) new_left).value) {
                return new BooleanConstant(true);
            } else {
                return right.removeConstants();
            }
        } else {
            Formula new_right = right.removeConstants();
            if (new_right instanceof BooleanConstant) {
                if (((BooleanConstant) new_right).value) {
                    return new BooleanConstant(true);
                } else {
                    return new_left;
                }
            } else {
                return new Disjunction(new_left, new_right);
            }
        }
    }

    @Override
    public boolean ignoresG(Formula f) {
//        return (!left.hasSubformula(f) || left.ignoresG(f))
//            && (!right.hasSubformula(f) || right.ignoresG(f));
        if (!hasSubformula(f)) {
            return true;
        } else {
            return left.ignoresG(f) && right.ignoresG(f);
        }
    }

    @Override
    public Formula toNNF() {
        return new Disjunction(left.toNNF(), right.toNNF());
    }

    @Override
    public Formula negationToNNF() {
        return new Conjunction(left.negationToNNF(), right.negationToNNF());
    }

    // ============================================================
    @Override
    public boolean isUnfoldOfF() {
        if (right instanceof XOperator) {
            if (((XOperator) right).operand instanceof FOperator) {
                return true;
            }
        }
        return false;
    }
    
    public BoolExpr toExpr(Context ctx){
    	BoolExpr l = left.toExpr(ctx);
    	BoolExpr r = right.toExpr(ctx);
    	
    	return ctx.mkOr(left.toExpr(ctx),right.toExpr(ctx));
    	
    }

	@Override
	public String toZ3String(boolean is_atom){
		String l=left.toZ3String(is_atom);
		String r= right.toZ3String(is_atom);
		if(l.equals("false")){
			if(r.equals("false")){
				return "false";
			}else{
				return r;
			}
		}else if(l.equals("true")){
			return "true";
		}else if(r.equals("true")){
			return "true";
		}else if(r.equals("false")){
			return l;
		}
		else if(is_atom){
			return l+"_or_"+r;
		}else{
			return "(or " +l+" "+r+")";
		}
	}

	@Override
	public ArrayList<String> getAllPropositions() {
		String l=left.toZ3String(true);
		String r= right.toZ3String(true);
		
		ArrayList<String> a=new ArrayList<String>();
		if(!l.equals("true")&& ! l.equals("false")){
			a.addAll(left.getAllPropositions());
		}
		if(!r.equals("true") && !r.equals("false")){
			a.addAll(right.getAllPropositions());
		}
		return a;
	}

	@Override
	public Formula rmAllConstants() {
		Formula l=left.rmAllConstants();
		Formula r=right.rmAllConstants();
		if(l instanceof BooleanConstant){
			if (((BooleanConstant) l).value){
				return new BooleanConstant(true);
			}else{
				return r;
			}
		}
		if(r instanceof BooleanConstant){
			if( ((BooleanConstant)r).value){
				return new BooleanConstant(true);
			}
			else{
				return l;
			}
		}
		return new Disjunction(l,r);
	}

}
