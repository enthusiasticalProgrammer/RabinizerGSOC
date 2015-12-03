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
public abstract class PropositionalFormula extends Formula {

    final Set<Formula> children;

    PropositionalFormula(Collection<Formula> children) {
        super();
        Set<Formula> tmp = new HashSet<Formula>();
        tmp.addAll(children);
        this.children = tmp;
    }

    public abstract Formula ThisTypeBoolean(Collection<Formula> children);

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
    public Formula removeConstants() {
        Set<Formula> newChildren = new HashSet<>();
        for (Formula child : children) {
            Formula newChild = child.removeConstants();
            newChildren.add(newChild);
        }
        if (!newChildren.equals(children)) {
            return ThisTypeBoolean(newChildren);
        }
        return this;
    }

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
    public Formula rmAllConstants() {
        Set<Formula> newChildren = new HashSet<>();
        newChildren.addAll(children);
        newChildren.stream().forEach(c -> c.rmAllConstants());

        if (!newChildren.equals(children)) {
            return ThisTypeBoolean(newChildren);
        }
        return this;
    }

    @Override
    public boolean ignoresG(Formula f) {

        if (!hasSubformula(f)) {
            return true;
        } else {
            return children.stream().allMatch(c -> c.ignoresG(f));
        }
    }

    @Override
    public Set<Formula> getPropositions() {
        Set<Formula> propositions = new HashSet<>();

        for (Formula child : children) {
            for (Formula proposition : child.getPropositions()) {
                if (!propositions.contains(proposition.not())) {
                    propositions.add(proposition);
                }
            }
        }

        return propositions;
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
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PropositionalFormula that = (PropositionalFormula) o;
        return Objects.equals(children, that.children);
    }

    @Override
    protected int hashCodeOnce() {
        return Objects.hash(children);
    }

    public abstract String operator();

    public Set<Formula> getChildren() {
        return Collections.unmodifiableSet(children);
    }

    @Override
    public Set<String> getAtoms() {
        Set<String> atoms = new HashSet<>();

        for (Formula child : children) {
            atoms.addAll(child.getAtoms());
        }

        return atoms;
    }
}
