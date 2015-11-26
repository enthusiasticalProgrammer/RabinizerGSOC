/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rabinizer.formulas;

import java.util.Set;


/**
 *
 * @author jkretinsky
 */
public abstract class FormulaBinary extends Formula {

    final Formula left, right;
    
    FormulaBinary(Formula left, Formula right, long id) {
    	super(id);
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FormulaBinary)) {
            return false;
        } else {
            return o.getClass().equals(getClass()) && ((FormulaBinary) o).left.unique_id==left.unique_id && ((FormulaBinary) o).right.unique_id==right.unique_id;
        }
    }
    
    @Override
    public String toString() {
        if (cachedString == null) {
            cachedString = "(" + left.toString() + operator() + right.toString() + ")";
        }
        return cachedString;
    }

    @Override
    public String toReversePolishString() {
        return operator() + " " + left.toReversePolishString() + " " + right.toReversePolishString();
    }
    
    @Override
    public boolean containsG() {
        return left.containsG() || right.containsG();
    }

    @Override
    public boolean hasSubformula(Formula f) {
        return this.equals(f) || left.hasSubformula(f) || right.hasSubformula(f);
    }

    @Override
    public Set<Formula> gSubformulas() {
        Set<Formula> r = left.gSubformulas();
        r.addAll(right.gSubformulas());
        return r;
    }

    @Override
    public Set<Formula> topmostGs() {
        Set<Formula> result = left.topmostGs();
        result.addAll(right.topmostGs());
        return result;
    }

}
