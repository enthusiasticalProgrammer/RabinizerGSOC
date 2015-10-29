package rabinizer.formulas;

import java.util.ArrayList;
import rabinizer.bdd.GSet;
import rabinizer.bdd.Valuation;
import rabinizer.z3.LTLExpr;
import java.util.*;
import com.microsoft.z3.*;


/**
 * @author Jan Kretinsky
 *
 */
public abstract class Formula {
	static int curr_symbol=0;
	
    protected String cachedString;
    protected LTLExpr cachedLTL=null;

    public abstract String operator();
   
    public LTLExpr ltl(){
    	if(cachedLTL==null){
    		cachedLTL=new LTLExpr(this);
    	}
    	return cachedLTL;
    }

    public Formula representative() {
        return LTLExpr.representative_of_formula(ltl(),this);
    }

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object o);

    public abstract String toReversePolishString();

    public abstract Formula toNNF();
    
    //to be overwritten, side-effects: propositions will be inserted into ctx
    public abstract BoolExpr toExpr(Context ctx);

    public abstract Formula negationToNNF();

    public abstract boolean containsG();

    public abstract boolean hasSubformula(Formula f);

    public abstract Set<Formula> gSubformulas();

    public abstract Set<Formula> topmostGs();

    // unfold everything, used in master automaton
    public abstract Formula unfold();

    // unfold everything but G's, used in slave automata
    public abstract Formula unfoldNoG();

    public Formula temporalStep(Valuation valuation) {
        return this.assertValuation(valuation).removeX();
    }

    public Formula assertValuation(Valuation valuation) {
        return evaluateValuation(valuation).removeConstants();
    }

    public Formula assertLiteral(Literal literal) {
        return evaluateLiteral(literal).removeConstants();
    }

    public Set<Formula> relevantGFormulas(Set<Formula> candidates) { // TODO: is with the outer G (not GSet)
        Set<Formula> result = new HashSet<Formula>();
        for (Formula subFormula : candidates) {
            if (hasSubformula(subFormula) && !unfold().representative().ignoresG(subFormula)) {
                result.add(subFormula);
            }
        }
        return result;
    }

    // is not recurrent and is not produced by anything recurrent from f
    // currently, the latter is safely approximated by having a different subformula
    public boolean isTransientwrt(Formula f) {
        return !containsG() && isVeryDifferentFrom(f);
    }

    // =============================================================
    // to be overridden by Boolean and Literal
    public Formula evaluateValuation(Valuation valuation) {
        return this;
    }

    // to be overridden by Boolean and Literal
    public Formula evaluateLiteral(Literal literal) {
        return this;
    }

    // to be overridden by Boolean and Literal
    public Literal getAnUnguardedLiteral() {
        return null;
    }

    // to be overridden by Boolean and XOperator
    public Formula removeX() {
        return this;
    }

    // to be overridden by Boolean and GOperator
    public Formula substituteGsToFalse(GSet gSet) {
        return this;
    }

    // to be overridden by Boolean
    public Formula removeConstants() {
        return this;
    }

    // to be overridden by Boolean
    // contains a modal/Literal not contained in f
    public boolean isVeryDifferentFrom(Formula f) {
        return !f.hasSubformula(this);
    }

    // to be overridden by Boolean
    public boolean ignoresG(Formula f) {
        //return false;
        return !hasSubformula(f);
    }

    // to be overridden by Disjunction
    public boolean isUnfoldOfF() {
        return false;
    }
    
    //to be overridden,
    //writes it to a string s.t. it can be interpreted by Z3
    //hint: this is not the only string the Z3 needs (also some preamble etc)
    //and this is only the first version.
    //it is likely not needed when calling the Z3 from the java interface
    public abstract String toZ3String(boolean is_atom);
    
    
    //to be overridden by subclasses
    //gets all propositions such as Fa, a, GaUb, ...
    public abstract ArrayList<String> getAllPropositions();
    
    
    //removeConstants did not remove boolean constants such as true/false
    //so I write this method
    public abstract Formula rmAllConstants();
    
    
    //simplifies a formula such that it is easier to compute for Rabinizer
    //used as f=f.simplifyLocally()
    public abstract Formula simplifyLocally();

}
