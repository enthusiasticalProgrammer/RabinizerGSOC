/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rabinizer.ltl;

import com.google.common.collect.ImmutableSet;
import rabinizer.automata.GSet;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author jkretinsky & Christopher Ziegler
 */
public abstract class PropositionalFormula extends Formula {

    final Set<Formula> children;

    protected PropositionalFormula(Collection<Formula> children) {
        this.children = ImmutableSet.copyOf(children);
    }

    protected PropositionalFormula(Formula... children) {
        this.children = ImmutableSet.copyOf(children);
    }

    public PropositionalFormula(Stream<Formula> formulaStream) {
        this.children = ImmutableSet.copyOf(formulaStream.iterator());
    }

    public abstract Formula ThisTypeBoolean(Collection<Formula> children);

    @Override
    public Formula unfold() {
        Set<Formula> unfolded = children.stream().map(Formula::unfold).collect(Collectors.toSet());
        return ThisTypeBoolean(unfolded);
    }

    @Override
    public Formula unfoldNoG() {
        Set<Formula> unfoldedNoG = children.stream().map(Formula::unfoldNoG).collect(Collectors.toSet());
        return ThisTypeBoolean(unfoldedNoG);
    }

    @Override
    public Formula evaluate(Set<String> valuation) {
        return ThisTypeBoolean(children.stream().map(c -> c.evaluate(valuation)).collect(Collectors.toSet()));
    }

    @Override
    public Formula evaluate(Literal literal) {
        return ThisTypeBoolean(children.stream().map(c -> c.evaluate(literal)).collect(Collectors.toSet()));
    }

    @Override
    public Formula removeX() {
        return ThisTypeBoolean(children.stream().map(Formula::removeX).collect(Collectors.toSet()));
    }

    @Override
    public Optional<Literal> getAnUnguardedLiteral() {
        for (Formula child : children) {
            Optional<Literal> literal = child.getAnUnguardedLiteral();

            if (literal.isPresent()) {
                return literal;
            }
        }

        return Optional.empty();
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
        Set<Formula> gSubstituted = children.stream().map(child -> child.substituteGsToFalse(gSet)).collect(Collectors.toSet());
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
        return children;
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
