/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rabinizer.automata;

import rabinizer.ltl.*;

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
    private final EquivalenceClassFactory factory;

    public GSet(EquivalenceClassFactory factory) {
        super();
        this.factory = factory;
    }

    public GSet(Collection<Formula> c, EquivalenceClassFactory factory) {
        super(c);
        this.factory = factory;
    }

    public boolean entails(Formula formula) { // used???
        if (gPremises == null) {
            Formula premise = BooleanConstant.get(true);
            for (Formula f : this) {
                premise = FormulaFactory.mkAnd(premise, FormulaFactory.mkG(f));

            }
            gPremises = premise;
        }

        EquivalenceClass thisClazz = factory.createEquivalenceClass(gPremises);
        EquivalenceClass thatClazz = factory.createEquivalenceClass(formula);

        return thisClazz.implies(thatClazz);

        //checks if gPremises (as BDD) implies formula
        // Context ctx = LTLExpr.getContext();
        // BoolExpr ant = gPremises.toExpr(ctx);
        // BoolExpr con = formula.toExpr(ctx);
        // Solver s = ctx.mkSolver();
        // s.add(ctx.mkAnd(ant, ctx.mkNot(con)));
        // return !(s.check() == Status.SATISFIABLE);

    }

}
