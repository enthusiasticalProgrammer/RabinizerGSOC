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

    public Conjunction(ArrayList<Formula> helper) {
    	super(null,null);
		if(helper.size()<2){
			throw new IllegalArgumentException();
		}else if(helper.size()==2){
			this.left=helper.get(0);
			this.right=helper.get(1);
		}else{
			this.right=new Conjunction(new ArrayList<Formula>(helper.subList((helper.size()-1)/2, helper.size())));
			if((helper.size()-1)/2==1){
				this.left=helper.get(0);
			}else{
				this.right=new Conjunction((ArrayList<Formula>)helper.subList(0,(helper.size()-1)/2-1));
			}
		}
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

	@Override
	public Formula rmAllConstants() {
		Formula l=left.rmAllConstants();
		Formula r=right.rmAllConstants();
		if(l instanceof BooleanConstant){
			if (((BooleanConstant) l).value){
				return r;
			}else{
				return new BooleanConstant(false);
			}
		}
		if(r instanceof BooleanConstant){
			if( ((BooleanConstant)r).value){
				return l;
			}
			else{
				return new BooleanConstant(false);
			}
		}
		return new Conjunction(l,r);
	}

	@Override
	public Formula simplifyLocally() {
		//first of all, get all subformulae beyound Conjunction(e.g. for c and (a and b)
		//I want a,b, and c, because you can simplify it more
		
		ArrayList<Formula> list=getAllChildrenOfConjunction();
		ArrayList<Formula> helper=new ArrayList<Formula>();
		for(int i=0;i<list.size();i++){
			list.set(i,list.get(i).simplifyLocally());
		}
		for(int i=list.size()-1;i>=0;i--){
			if(list.get(i) instanceof BooleanConstant){	
				if(!((BooleanConstant) list.get(i)).value){
					return new BooleanConstant(false);
				}
				list.remove(i);
			}
		}
		
		//put all G's together
		for(int i=list.size()-1;i>=0;i--){
			if(list.get(i) instanceof GOperator){
				helper.add(list.get(i));
				list.remove(i);
			}
		}
		if(helper.size()>1){
			for(int i=0;i<helper.size();i++){
				helper.set(i, ((GOperator) helper.get(i)).operand);
			}
			list.add(new GOperator(new Conjunction(helper)).simplifyLocally());
		}else if(helper.size()==1){
			list.add(helper.get(0));
		}
		helper.clear();
		
		//put all X's together
		for(int i=list.size()-1;i>=0;i--){
			if(list.get(i) instanceof XOperator){
				helper.add(list.get(i));
				list.remove(i);
			}
		}
		if(helper.size()>1){
			for(int i=0;i<helper.size();i++){
				helper.set(i, ((XOperator) helper.get(i)).operand);
				
			}
			list.add(new XOperator(new Conjunction(helper)).simplifyLocally());
		}else if(helper.size()==1){
			list.add(helper.get(0));
		}
		helper.clear();
		
		
		//put all Literals together (and check for trivial tautologies/contradictions like a and a /a and !a
		for(int i=list.size()-1;i>=0;i--){
			if(list.get(i) instanceof Literal){
				helper.add(list.get(i));
				list.remove(i);
				
			}
		}
		for(int i=0;i<helper.size();i++){
			
			for(int j=i+1;j<helper.size();j++){
				if(((Literal) helper.get(i)).atom.equals(((Literal) helper.get(j)).atom)){
					if(((Literal) helper.get(i)).negated==(((Literal) helper.get(j)).negated)){
						helper.remove(j);
					}else{
						return new BooleanConstant(false);
					}
				}
			}
		}
		list.addAll(helper);
		
		if(list.size()==0){
			return new BooleanConstant(true);
		}else if(list.size()==1){
			return list.get(0);
		}else{
			return new Conjunction(list);
		}
		
	}
	
	//helps for simplifyLocally()
	private ArrayList<Formula> getAllChildrenOfConjunction(){
		ArrayList<Formula> l=new ArrayList<Formula>();
		if(left instanceof Conjunction){
			l.addAll(((Conjunction)left).getAllChildrenOfConjunction());
		}else{
			if(left instanceof Negation){
				if(((Negation)left).operand instanceof Disjunction){
					left=new Conjunction(new Negation(((Disjunction)((Negation) left).operand).left),new Negation(((Disjunction)((Negation) left).operand).right));
					l.addAll(((Conjunction)left).getAllChildrenOfConjunction());
				}else{
					l.add(left);
				}
			}else{
				l.add(left);
			}
		}
		if(right instanceof Conjunction){
			l.addAll(((Conjunction) right).getAllChildrenOfConjunction());
		}else{
			if(right instanceof Negation){
				if(((Negation)right).operand instanceof Disjunction){
					right=new Conjunction(new Negation(((Disjunction)((Negation) right).operand).left),new Negation(((Disjunction)((Negation) right).operand).right));
					l.addAll(((Conjunction)right).getAllChildrenOfConjunction());
				}else{
					l.add(right);
				}
			}else{
				l.add(right);
			}
		}
		return l;
	}

}
