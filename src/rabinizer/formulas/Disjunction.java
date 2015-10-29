package rabinizer.formulas;

import net.sf.javabdd.*;
import rabinizer.bdd.BDDForFormulae;

import java.util.ArrayList;

import com.microsoft.z3.*;

/**
 * @author Andreas Gaiser & Ruslan Ledesma-Garza
 *
 *
 *
 */
public class Disjunction extends FormulaBinaryBoolean {

    public Disjunction(Formula left, Formula right) {
        super(left, right);
    }

    public Disjunction(ArrayList<Formula> helper) {
    	super(null,null);
		if(helper.size()<2){
			throw new IllegalArgumentException();
		}else if(helper.size()==2){
			this.left=helper.get(0);
			this.right=helper.get(1);
		}
		else{
			this.right=new Disjunction(new ArrayList<Formula>(helper.subList((helper.size()-1)/2, helper.size())));
			if((helper.size()-1)/2==1){
				this.left=helper.get(0);
			}else{
				this.right=new Disjunction((ArrayList<Formula>)helper.subList(0,(helper.size()-1)/2-1));
			}
		}
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
	
	@Override
	public Formula simplifyLocally() {
		//first of all, get all subformulae beyound Conjunction(e.g. for c or (a or b)
		//I want a,b, and c, because you can simplify it more
		
		ArrayList<Formula> list=getAllChildrenOfDisjunction();
		ArrayList<Formula> helper=new ArrayList<Formula>();
		for(int i=0;i<list.size();i++){
			list.set(i,list.get(i).simplifyLocally());
		}
		
		for(int i=list.size()-1;i>=0;i--){
			
			if(list.get(i) instanceof BooleanConstant){
				
				if(((BooleanConstant) list.get(i)).value){
					return new BooleanConstant(true);
				}
				list.remove(i);
			}
		}
		
		//put all F's together
		for(int i=list.size()-1;i>=0;i--){
			if(list.get(i) instanceof FOperator){
				helper.add(list.get(i));
				list.remove(i);
				
			}
		}
		if(helper.size()>1){
			for(int i=0;i<helper.size();i++){
				helper.set(i, ((FOperator) helper.get(i)).operand);
			}
			list.add(new FOperator(new Disjunction(helper)).simplifyLocally());
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
			list.add(new XOperator(new Disjunction(helper)).simplifyLocally());
		}else if(helper.size()==1){
			list.add(helper.get(0).simplifyLocally());
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
						return new BooleanConstant(true);
					}
				}
			}
		}
		list.addAll(helper);
		//System.out.println("Children: "+list.toString());
		if(list.size()==0){
			return new BooleanConstant(true);
		}else if(list.size()==1){
			return list.get(0);
		}else{
			return new Disjunction(list);
		}
		
	}

	private ArrayList<Formula> getAllChildrenOfDisjunction() {
		ArrayList<Formula> l=new ArrayList<Formula>();
		if(left instanceof Disjunction){
			l.addAll(((Disjunction)left).getAllChildrenOfDisjunction());
		}else{
			if(left instanceof Negation){
				if(((Negation)left).operand instanceof Conjunction){
					left=new Disjunction(new Negation(((Conjunction)((Negation) left).operand).left),new Negation(((Conjunction)((Negation) left).operand).right));
					l.addAll(((Disjunction)left).getAllChildrenOfDisjunction());
				}else{
					l.add(left);
				}
			}else{
				l.add(left);
			}
		}
		if(right instanceof Disjunction){
			l.addAll(((Disjunction) right).getAllChildrenOfDisjunction());
		}else{
			if(right instanceof Negation){
				if(((Negation)right).operand instanceof Conjunction){
					right=new Disjunction(new Negation(((Conjunction)((Negation) right).operand).left),new Negation(((Conjunction)((Negation) right).operand).right));
					l.addAll(((Disjunction)right).getAllChildrenOfDisjunction());
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
