/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rabinizer.bdd;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;
import rabinizer.formulas.Formula;
import rabinizer.formulas.FormulaFactory;
import rabinizer.z3.LTLExpr;

import java.util.Collection;
import java.util.HashSet;


/**
 * @author jkretinsky
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
            Formula premise = FormulaFactory.mkConst(true);
            for (Formula f : this) {
                premise = FormulaFactory.mkAnd(premise, FormulaFactory.mkG(f));

            }
            gPremises = premise;
        }
        //checks if gPremises (as BDD) implies formula
        Context ctx = LTLExpr.getContext();
        BoolExpr ant = gPremises.toExpr(ctx);
        BoolExpr con = formula.toExpr(ctx);
        Solver s = ctx.mkSolver();
        s.add(ctx.mkAnd(ant, ctx.mkNot(con)));
        return !(s.check() == Status.SATISFIABLE);

    }

}
