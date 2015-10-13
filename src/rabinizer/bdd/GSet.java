/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rabinizer.bdd;

import java.util.*;
import net.sf.javabdd.BDD;
import rabinizer.bdd.BDDForFormulae;
import rabinizer.formulas.BooleanConstant;
import rabinizer.formulas.Conjunction;
import rabinizer.formulas.Formula;
import rabinizer.formulas.GOperator;


/**
 *
 * @author jkretinsky
 */
public class GSet extends HashSet<Formula> {

    /**
	 * 
	 */
	private static final long serialVersionUID = 6122181497119884736L;
	private BDD gPremises = null;

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
            gPremises = premise.bdd();
        }
        //checks if gPremises (as BDD) implies formula
        return gPremises.imp(formula.bdd()).equals(BDDForFormulae.bddFactory.one());
    }

}
