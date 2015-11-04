package rabinizer.formulas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import rabinizer.exec.Main;

public class FormulaFactory {

	private static HashMap<Formula,Formula> formulae=new HashMap<Formula,Formula>();
	private static long next_identifier=0;
	
	
	//For and/or, the output of the formula can be reverted
	// because this makes Comparable easier
	public static Formula mkAnd(Formula... af){
		Formula swap;
		ArrayList<Formula> helper=new ArrayList<Formula>();
		for(int i=0;i<af.length;i++){
			if(af[i] instanceof Conjunction){
				helper.addAll(((Conjunction)af[i]).children);
			}else{
				helper.add(af[i]);
			}
		}
		
		for(int i=0;i<helper.size();i++){
			for(int j=helper.size()-1;j>i;j--){
				if(helper.get(i).unique_id>helper.get(j).unique_id){
					swap=helper.get(i);
					helper.set(i,helper.get(j));
					helper.set(j,swap);
				}else if(helper.get(i).unique_id==helper.get(j).unique_id){
					helper.remove(j);
				}
			}
		}
		
		if(helper.size()==0){
			return FormulaFactory.mkConst(true);
		}else if(helper.size()==1){
			return helper.get(0);
		}
		
		Formula z=(new Conjunction(helper,next_identifier++)).simplifyLocally();
		Formula inMap=formulae.get(z);
		if(inMap==null){
			formulae.put(z,z);
			return z;
		}else{
			return inMap;
		}

	}
	
	public static Formula mkOr(Formula... af){
		Formula swap;
		ArrayList<Formula> helper=new ArrayList<Formula>();
		
		for(int i=0;i<af.length;i++){
			if(af[i] instanceof Disjunction){
				helper.addAll(((Disjunction)af[i]).children);
			}else{
				helper.add(af[i]);
			}
		}
		for(int i=0;i<helper.size();i++){
			for(int j=helper.size()-1;j>i;j--){
				if(helper.get(i).unique_id>helper.get(j).unique_id){
					swap=helper.get(i);
					helper.set(i,helper.get(j));
					helper.set(j,swap);
				}else if(helper.get(i).unique_id==helper.get(j).unique_id){
					helper.remove(j);
				}
			}
		}
		if(helper.size()==0){
			return FormulaFactory.mkConst(false);
		}else if(helper.size()==1){
			return helper.get(0);
		}
		Formula z=(new Disjunction(helper,next_identifier++)).simplifyLocally();
		Formula inMap=formulae.get(z);
		if(inMap==null){
			formulae.put(z,z);
			return z;
		}else{
			return inMap;
		}

	}
	
	public static Formula mkAnd(ArrayList<Formula> af){
		Formula[] helper=new Formula[af.size()];
		helper=af.toArray(helper);
		return mkAnd(helper);
	}
	
	public static Formula mkOr(ArrayList<Formula> af){
		Formula[] helper=new Formula[af.size()];
		helper=af.toArray(helper);
		return mkOr(helper);
	}
	
	
	
	public static Formula mkConst(boolean t){
		Formula z=new BooleanConstant(t,next_identifier);
		Formula inMap=formulae.get(z);
		if(inMap==null){
			formulae.put(z,z);
			next_identifier++;
			return z;
		}else{
			return inMap;
		}
	}
	
	public static Formula mkF(Formula child){
		Formula z=new FOperator(child,next_identifier++).simplifyLocally();
		Formula inMap=formulae.get(z);
		if(inMap==null){
			formulae.put(z, z);
			return z;
		}else{
			return inMap;
		}
	}
	
	public static Formula mkG(Formula child){
		Formula z=new GOperator(child,next_identifier++).simplifyLocally();
		Formula inMap=formulae.get(z);
		if(inMap==null){
			formulae.put(z, z);
			return z;
		}else{
			return inMap;
		}
	}
	
	public static Formula mkLit(String proposition, int atomId,boolean negated){
		Formula z=new Literal(proposition,atomId,negated,next_identifier);
		Formula inMap=formulae.get(z);
		if(inMap==null){
			formulae.put(z, z);
			next_identifier++;
			return z;
		}else{
			return inMap;
		}
	}
	
	public static Formula mkNot(Formula child){
		Formula z=new Negation(child,next_identifier++).simplifyLocally();
		Formula inMap=formulae.get(z);
		if(inMap==null){
			formulae.put(z, z);
			return z;
		}else{
			return inMap;
		}
	}
	
	public static Formula mkU(Formula l, Formula r){
		Formula z=new UOperator(l,r,next_identifier++).simplifyLocally();
		Formula inMap=formulae.get(z);
		if(inMap==null){
			formulae.put(z, z);
			return z;
		}else{
			return inMap;
		}
	}
	
	public static Formula mkX(Formula child){
		Formula z=new XOperator(child,next_identifier++).simplifyLocally();
		Formula inMap=formulae.get(z);
		if(inMap==null){
			formulae.put(z, z);
			return z;
		}else{
			return inMap;
		}
	}
}
