/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rabinizer.ltl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author jkretinsky
 */
public abstract class FormulaNullary extends Formula {

    @Override
    public Formula unfold() {
        return this;
    }

    @Override
    public Formula unfoldNoG() {
        return this;
    }

    @Override
    public boolean hasSubformula(Formula f) {
        return this.equals(f);
    }

    @Override
    public Set<Formula> gSubformulas() {
        return new HashSet<>();
    }

    @Override
    public Set<Formula> topmostGs() {
        return Collections.emptySet();
    }

    @Override
    public boolean containsG() {
        return false;
    }
}
