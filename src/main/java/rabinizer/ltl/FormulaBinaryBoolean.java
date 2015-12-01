/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rabinizer.ltl;


import rabinizer.ltl.bdd.GSet;
import rabinizer.ltl.bdd.Valuation;

import java.util.*;

/**
 * @author jkretinsky & Christopher Ziegler
 */
public abstract class FormulaBinaryBoolean extends Formula { /* Why is this called Binary? */

    final Set<Formula> children;

    FormulaBinaryBoolean(Collection<Formula> children) {
        this.children = new HashSet<>(children);
    }

    public abstract Formula ThisTypeBoolean(ArrayList<Formula> children);

    @Override
    public Formula unfold() {
        ArrayList<Formula> unfolded = new ArrayList<>();
        for (Formula child : children) {
            unfolded.add(child.unfold());
        }
        return ThisTypeBoolean(unfolded);
    }

    @Override
    public Formula unfoldNoG() {
        ArrayList<Formula> unfoldedNoG = new ArrayList<>();
        for (Formula child : children) {
            unfoldedNoG.add(child.unfoldNoG());
        }
        return ThisTypeBoolean(unfoldedNoG);
    }

    @Override
    public Formula evaluateValuation(Valuation valuation) {
        ArrayList<Formula> evaluated = new ArrayList<>();
        for (Formula child : children) {
            evaluated.add(child.evaluateValuation(valuation));
        }
        return ThisTypeBoolean(evaluated);
    }

    @Override
    public Formula evaluateLiteral(Literal literal) {
        ArrayList<Formula> evaluated = new ArrayList<>();
        for (Formula child : children) {
            evaluated.add(child.evaluateLiteral(literal));
        }
        return ThisTypeBoolean(evaluated);
    }

    @Override
    public abstract Formula removeConstants();

    @Override
    public Formula removeX() {
        ArrayList<Formula> xRemoved = new ArrayList<>();
        for (Formula child : children) {
            xRemoved.add(child.removeX());
        }
        return ThisTypeBoolean(xRemoved);
    }

    @Override
    public Literal getAnUnguardedLiteral() {
        for (Formula child : children) {
            if (child.getAnUnguardedLiteral() != null) {
                return child.getAnUnguardedLiteral();
            }
        }
        return null;
    }

    @Override
    public Set<Formula> topmostGs() {
        Set<Formula> result = new HashSet<>();
        for (Formula child : children) {
            result.addAll(child.topmostGs());
        }
        return result;
    }

    @Override
    public ArrayList<String> getAllPropositions() {
        ArrayList<String> a = new ArrayList<>();
        for (Formula child : children) {
            a.addAll(child.getAllPropositions());
        }

        return a;
    }

    @Override
    public Formula substituteGsToFalse(GSet gSet) {
        ArrayList<Formula> gSubstituted = new ArrayList<>();
        for (Formula child : children) {
            gSubstituted.add(child.substituteGsToFalse(gSet));
        }
        return ThisTypeBoolean(gSubstituted);
    }

    @Override
    public Set<Formula> gSubformulas() {
        Set<Formula> gSub = new HashSet<>();
        for (Formula child : children) {
            gSub.addAll(child.gSubformulas());
        }
        return gSub;
    }

    @Override
    public boolean hasSubformula(Formula f) {
        boolean subform = this.equals(f);
        for (Formula child : children) {
            subform = subform || child.hasSubformula(f);
        }
        return subform;
    }

    @Override
    public boolean containsG() {
        boolean contG = false;
        for (Formula child : children) {
            contG = contG || child.containsG();
        }
        return contG;
    }

    @Override
    public boolean isVeryDifferentFrom(Formula f) {
        boolean diff = false;
        for (Formula child : children) {
            diff = diff || child.isVeryDifferentFrom(f);
        }
        return diff;
    }

    @Override
    public String toString() {
        if (cachedString == null) {
            cachedString = "(";
            for (Formula child : children) {
                cachedString = cachedString + (cachedString.equals("(") ? "" : operator()) + child;
            }
            cachedString = cachedString + ")";

        }
        return cachedString;
    }

    @Override
    public abstract boolean ignoresG(Formula f);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FormulaBinaryBoolean that = (FormulaBinaryBoolean) o;
        return Objects.equals(children, that.children);
    }

    @Override
    protected int hashCodeOnce() {
        return Objects.hash(children);
    }

    public abstract String operator();
}
