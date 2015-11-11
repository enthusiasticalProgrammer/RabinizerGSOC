/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rabinizer.formulas;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rabinizer.bdd.GSet;
import rabinizer.bdd.Valuation;

/**
 *
 * @author jkretinsky & Christopher Ziegler
 */
public abstract class FormulaBinaryBoolean extends Formula{

	final List<Formula> children;
    FormulaBinaryBoolean(List<Formula> children, long id) {
        super(id);
        this.children=children;
    }

    public abstract Formula ThisTypeBoolean(ArrayList<Formula> children);

    @Override
    public Formula unfold() {
    	ArrayList<Formula> unfolded=new ArrayList<Formula>();
    	for( Formula child:children){
    		unfolded.add(child.unfold());
    	}
        return ThisTypeBoolean(unfolded);
    }

    @Override
    public Formula unfoldNoG() {
    	ArrayList<Formula> unfoldedNoG=new ArrayList<Formula>();
    	for( Formula child:children){
    		unfoldedNoG.add(child.unfoldNoG());
    	}
        return ThisTypeBoolean(unfoldedNoG);
    }

    @Override
    public Formula evaluateValuation(Valuation valuation) {
    	ArrayList<Formula> evaluated=new ArrayList<Formula>();
    	for( Formula child:children){
    		evaluated.add(child.evaluateValuation(valuation));
    	}
        return ThisTypeBoolean(evaluated);
    }

    @Override
    public Formula evaluateLiteral(Literal literal) {
    	ArrayList<Formula> evaluated=new ArrayList<Formula>();
    	for( Formula child:children){
    		evaluated.add(child.evaluateLiteral(literal));
    	}
        return ThisTypeBoolean(evaluated);
    }

    @Override
    public abstract Formula removeConstants();

    @Override
    public Formula removeX() {
    	ArrayList<Formula> xRemoved=new ArrayList<Formula>();
    	for( Formula child:children){
    		xRemoved.add(child.removeX());
    	}
        return ThisTypeBoolean(xRemoved);
    }

    @Override
    public Literal getAnUnguardedLiteral() {
    	for(Formula child:children){
    		if(child.getAnUnguardedLiteral()!=null){
    			return child.getAnUnguardedLiteral();
    		}
    	}
    	return null;
    }

    @Override
    public Set<Formula> topmostGs() {
    	Set<Formula> result=new HashSet<Formula>();
    	for(Formula child: children){
    		result.addAll(child.topmostGs());
    	}
        return result;
    }
    	
    @Override
    public String toReversePolishString() {
    	String result=operator();
    	for(Formula child:children){
    		result=result+" "+child.toReversePolishString();
    	}
        return result;
    }
    
	@Override
	public ArrayList<String> getAllPropositions() {
		ArrayList<String> a=new ArrayList<String>();
		for(Formula child:children){
			a.addAll(child.getAllPropositions());
		}

		return a;
	}
    
    @Override
    public Formula substituteGsToFalse(GSet gSet) {
    	ArrayList<Formula> gSubstituted=new ArrayList<Formula>();
    	for( Formula child:children){
    		gSubstituted.add(child.substituteGsToFalse(gSet));
    	}
        return ThisTypeBoolean(gSubstituted);
    }
    
    @Override
    public Set<Formula> gSubformulas() {
    	Set<Formula> gSub=new HashSet<Formula>();
    	for(Formula child: children){
    		gSub.addAll(child.gSubformulas());
    	}
        return gSub;
    }

    @Override
    public boolean hasSubformula(Formula f) {
    	boolean subform=this.equals(f);
    	for(Formula child: children){
    		subform=subform || child.hasSubformula(f);
    	}
        return subform;
    }
    
    @Override
    public boolean containsG() {
    	boolean contG=false;
    	for(Formula child: children){
    		contG=contG||child.containsG();
    	}
        return contG;
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FormulaBinaryBoolean)) {
            return false;
        } else {
        	if(!o.getClass().equals(getClass())||o.hashCode()!=this.hashCode()){
        		return false;
        	}
        	
        	if(((FormulaBinaryBoolean)o).children.size()!=this.children.size()){
        		return false;
        	}
        	
        	for(int i=0;i<this.children.size();i++){
        		if(this.children.get(i).get_id()!=((FormulaBinaryBoolean)o).children.get(i).get_id()){
        			return false;
        		}
        	}
        	
            return true;
        }
    }
    
    @Override
    public boolean isVeryDifferentFrom(Formula f) {
    	boolean diff=false;
    	for(Formula child:children){
    		diff=diff || child.isVeryDifferentFrom(f);
    	}
        return diff;
    }

    @Override
    public String toString() {
        if (cachedString == null) {
        	cachedString = "(";
        	for(Formula child: children){
        		cachedString=cachedString+(cachedString.equals("(") ? "":operator())+child.toString();
        	}
        	cachedString=cachedString+")";
            
        }
        return cachedString;
    }
    
    
    @Override
    public abstract boolean ignoresG(Formula f);
    
    
	@Override
	public Formula setToConst(long id, boolean constant) {
		if(id==unique_id){
			return FormulaFactory.mkConst(constant);
		}
		ArrayList<Formula> helper=new ArrayList<Formula>();
		for(Formula child:children){
			helper.add(child.setToConst(id, constant));
		}
		boolean eq=true;
		for(int i=0;i<helper.size();i++){
			eq=eq&&helper.get(i)==children.get(i);
		}
		return ThisTypeBoolean(helper);
	}

}
