package rabinizer.formulas;

import java.util.ArrayList;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;

import net.sf.javabdd.BDD;
import rabinizer.bdd.BDDForFormulae;


/**
 * @author Andreas Gaiser & Ruslan Ledesma-Garza
 * & Christopher Ziegler
 *
 */
public class Conjunction extends FormulaBinaryBoolean {

    public Conjunction(Formula left, Formula right) {
        super(left, right);
    }

    @Override
    public Conjunction ThisTypeBoolean(Formula left, Formula right) {
        return new Conjunction(left, right);
    }

    @Override
    public String operator() {
        return "&";
    }

    @Override
    public BDD bdd() {
        if (cachedBdd == null) {
            cachedBdd = left.bdd().and(right.bdd());
            BDDForFormulae.representativeOfBdd(cachedBdd, this);
        }
        return cachedBdd;
    }

    @Override
    public Formula removeConstants() {
        Formula new_left = left.removeConstants();
        if (new_left instanceof BooleanConstant) {
            if (((BooleanConstant) new_left).value) {
                return right.removeConstants();
            } else {
                return new BooleanConstant(false);
            }
        } else {
            Formula new_right = right.removeConstants();
            if (new_right instanceof BooleanConstant) {
                if (((BooleanConstant) new_right).value) {
                    return new_left;
                } else {
                    return new BooleanConstant(false);
                }
            } else {
                return new Conjunction(new_left, new_right);
            }
        }
    }

    @Override
    public boolean ignoresG(Formula f) {
//        if (!left.isVeryDifferentFrom(right)) {
//            //System.out.println("$$$$$$not very different$$$$"+this);
//            return false;
//        }
//        if (left.equals(f.unfold()) && right.isUnfoldOfF() && !right.containsG()
//            || right.equals(f.unfold()) && left.isUnfoldOfF() && !left.containsG()) {
//            //System.out.println("$$$$$$"+left+"$$$$"+right+"$$$susp "+f);
//            return true;					// independent waiting formula
//        } else {
//            return left.ignoresG(f) && right.ignoresG(f); 	// don't know yet
//        }
        if (!hasSubformula(f) || left.isTransientwrt(right) || right.isTransientwrt(left)) {
            return true;
        } else {
            return left.ignoresG(f) && right.ignoresG(f); 	// don't know yet
        }
    }

    @Override
    public Formula toNNF() {
        return new Conjunction(left.toNNF(), right.toNNF());
    }

    @Override
    public Formula negationToNNF() {
        return new Disjunction(left.negationToNNF(), right.negationToNNF());
    }
    
    public BoolExpr toExpr(Context ctx){
    	BoolExpr l=left.toExpr(ctx);
    	BoolExpr r=right.toExpr(ctx);
    	if(l.isTrue()){
    		return r;
    	}else if(r.isTrue()){
    		return l;
    	}else if(l.isFalse()|| r.isFalse()){
    		return ctx.mkFalse();
    	}
    	return ctx.mkAnd(l,r);
    }

    
	@Override
	public String toZ3String(boolean is_atom) {
		String l=left.toZ3String(is_atom);
		String r= right.toZ3String(is_atom);
		if(l.equals("true")){
			if(r.equals("true")){
				return "true";
			}else if(r.equals("false")){
				return "false";
			}else{
				return r;
			}
		}else if(l.equals("false")){
			return "false";
		}else if(r.equals("true")){
			return l;
		}else if(r.equals("false")){
			return "false";
		}
		else if(is_atom){
			return l+"&"+r;
		}else{
			return "(and " +l+" "+r+")";
		}
	}

	@Override
	public ArrayList<String> getAllPropositions() {
		String l=left.toZ3String(true);
		String r=right.toZ3String(true);
		ArrayList<String> a=new ArrayList<String>();
		
		if(!l.equals("true") && !l.equals("false")){
			a.addAll(left.getAllPropositions());
		}
		if(!r.equals("true") && !r.equals("false")){
			a.addAll(right.getAllPropositions());
		}
		return a;
	}

}
