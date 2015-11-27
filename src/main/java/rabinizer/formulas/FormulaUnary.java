/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package rabinizer.formulas;

import net.sf.javabdd.BDD;
import rabinizer.bdd.BDDForFormulae;

import java.util.Set;

/**
 * @author jkretinsky
 */
public abstract class FormulaUnary extends Formula {

    final Formula operand;

    FormulaUnary(Formula operand, long id) {
        super(id);
        this.operand = operand;
    }

    //might also be a Boolean constant or sth else,
    //since FormulaFactory simplifies upon creation
    public abstract Formula ThisTypeUnary(Formula operand);

    @Override
    public BDD bdd() {
        if (cachedBdd == null) {
            Formula booleanAtom = ThisTypeUnary(operand.representative());
            int bddVar = BDDForFormulae.bijectionBooleanAtomBddVar.id(booleanAtom);
            if (BDDForFormulae.bddFactory.varNum() <= bddVar) {
                BDDForFormulae.bddFactory.extVarNum(1);
            }
            cachedBdd = BDDForFormulae.bddFactory.ithVar(bddVar);
            BDDForFormulae.representativeOfBdd(cachedBdd, this);
        }
        return cachedBdd;
    }


    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FormulaUnary)) {
            return false;
        } else {
            return o.getClass().equals(getClass()) && ((FormulaUnary) o).operand.unique_id == operand.unique_id;
        }
    }

    @Override
    public String toString() {
        if (cachedString == null) {
            cachedString = operator() + operand;
        }
        return cachedString;
    }

    @Override
    public String toReversePolishString() {
        return operator() + " " + operand.toReversePolishString();
    }

    @Override
    public boolean hasSubformula(Formula f) {
        return this.equals(f) || operand.hasSubformula(f);
    }

    @Override
    // to be overrridden by GOperator
    public boolean containsG() {
        return operand.containsG();
    }

    @Override
    // to be overrridden by GOperator
    public Set<Formula> gSubformulas() {
        return operand.gSubformulas();
    }

    @Override
    // to be overrridden by GOperator
    public Set<Formula> topmostGs() {
        return operand.topmostGs();
    }


    @Override
    //to be overridden by FOperator
    public Formula setToConst(long id, boolean constant) {
        if (unique_id == id) {
            return FormulaFactory.mkConst(constant);
        } else {
            return this;
        }
    }

}
