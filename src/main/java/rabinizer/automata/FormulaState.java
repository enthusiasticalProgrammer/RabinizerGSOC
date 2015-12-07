/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package rabinizer.automata;

import rabinizer.ltl.EquivalenceClass;
import rabinizer.ltl.Formula;

import java.util.Objects;

/**
 * @author jkretinsky
 */
public class FormulaState {

    final protected EquivalenceClass clazz;

    public FormulaState(EquivalenceClass clazz) {
        this.clazz = clazz;
    }

    @Override
    public String toString() {
        return clazz.getRepresentative().toString();
    }

    public Formula getFormula() {
        return clazz.getRepresentative();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FormulaState)) {
            return false;
        } else {
            return clazz.equals(((FormulaState) o).clazz);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(clazz);
    }
}
