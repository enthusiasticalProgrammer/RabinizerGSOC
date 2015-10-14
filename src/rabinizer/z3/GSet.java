/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rabinizer.z3;

import java.util.*;

import com.microsoft.z3.*;


import rabinizer.formulas.BooleanConstant;
import rabinizer.formulas.Conjunction;
import rabinizer.formulas.Formula;
import rabinizer.formulas.GOperator;


/**
 *
 * @author jkretinsky & Christopher Ziegler
 */
public class GSet extends HashSet<Formula> {

    /**
	 * 
	 */
	private static final long serialVersionUID = 6122181497119884736L;
	private Formula gPremises = null;

    public GSet() {
        super();
    }

    public GSet(Collection<Formula> c) {
        super(c);
    }

    public boolean entails(Formula formula) { // used???
        if (gPremises == null) {
            Formula premise = new BooleanConstant(true);
            for (Formula f : this) {
                premise = new Conjunction(premise, new GOperator(f));

            }
            gPremises = premise;
        }
        //checks if gPremises (as BDD) implies formula
        Context ctx=new Context();
        BoolExpr ant=gPremises.toExpr(ctx);
        BoolExpr con=formula.toExpr(ctx);
        Solver s=ctx.mkSolver();
    	s.add(ctx.mkAnd(ant,ctx.mkNot(con)));
    	boolean result=!(s.check()==Status.SATISFIABLE);
        
        ctx.dispose();
        return result;
    }

}
