package rabinizer.z3;
import java.util.HashMap;
import java.util.Map;

import com.microsoft.z3.*;

import rabinizer.formulas.Formula;


/**
 * @author Christopher Ziegler
 *
 */


//problem: this class has a constant hashCode(to me: either constant or NP-hard!)
public class LTLExpr{

	//in order to communicate properly with the Z3-Solver,
	// e.g. to store the atomar propositions
	private static Context ctx=null;
	
	
	//stores all Formulae, gets emptied in dispose-Method
	private static Map<LTLExpr, Formula> LTLToRepresentative=new HashMap<LTLExpr, Formula>();

	
	//propositional formula, in order to have a representative
	//with which you can compare two formulae propositionally
	private BoolExpr prop_form;
	

	public LTLExpr(Formula f){
		if(ctx==null){
			ctx=new Context();
		}
		prop_form=f.toExpr(ctx);
		prop_form.simplify();
	}
	
	@Override
	public boolean equals(Object o){
		
		if(!(o instanceof LTLExpr)){
			return false;
		}
		LTLExpr ltl=(LTLExpr) o;
		Solver s=ctx.mkSolver();
    	s.add(ctx.mkNot(ctx.mkEq(this.prop_form, ltl.prop_form)));
    	return !(s.check()==Status.SATISFIABLE);
	}
	
	@Override
	public String toString(){
		return prop_form.toString();
	}
	
	public static void dispose(){
		ctx.dispose();
		ctx=null;
		LTLToRepresentative.clear();
	}

	public static Formula representative_of_formula(LTLExpr ltl, Formula candidate){
		
		if(!LTLToRepresentative.containsKey(ltl)){
			LTLToRepresentative.put(ltl,candidate);
		}

		return LTLToRepresentative.get(ltl);
	}
	
	@Override
	public int hashCode() {
	   return 3141;
	}

	
	
	public static Context getContext(){
		if(ctx==null){
			ctx=new Context();
		}
		return ctx;
	}

}
