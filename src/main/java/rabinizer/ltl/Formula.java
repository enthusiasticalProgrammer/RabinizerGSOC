package rabinizer.ltl;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import rabinizer.automata.GSet;
import rabinizer.ltl.bdd.BDDEquivalenceClassFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Jan Kretinsky
 */
public abstract class Formula {

    String cachedString;
    BoolExpr cachedLTL = null;
    private int cachedHashCode = 0;

    @Override
    public int hashCode() {
        if (cachedHashCode == 0) {
            cachedHashCode = hashCodeOnce();
        }

        return cachedHashCode;
    }

    protected abstract int hashCodeOnce();

    @Override
    public abstract boolean equals(Object o);

    public abstract boolean containsG();

    public abstract BoolExpr toExpr(Context ctx);

    public abstract boolean hasSubformula(Formula f);

    public abstract Set<Formula> gSubformulas();

    public abstract Set<Formula> topmostGs();

    // unfold everything, used in master automaton
    public abstract Formula unfold();

    // unfold everything but G's, used in slave automata
    public abstract Formula unfoldNoG();

    public Formula temporalStep(Set<String> valuation) {
        return this.assertValuation(valuation).removeX();
    }

    public Formula assertValuation(Set<String> valuation) {
        return evaluateValuation(valuation).removeConstants();
    }

    public Formula assertLiteral(Literal literal) {
        return evaluateLiteral(literal).removeConstants();
    }

    // TODO: is with the outer G (not GSet)
    public Set<Formula> relevantGFormulas(Set<Formula> candidates) {
        // TODO: Remove adhoc hack to compute relevant G -> move EquivClass
        if (getPropositions().isEmpty()) {
            return Collections.emptySet();
        }
        Set<Formula> result = new HashSet<>();

        EquivalenceClassFactory factory = new BDDEquivalenceClassFactory(getPropositions());
        EquivalenceClass clazz = factory.createEquivalenceClass(this.unfold());

        for (Formula subFormula : candidates) {
            if (hasSubformula(subFormula) && !clazz.getSimplifiedRepresentative().ignoresG(subFormula)) {
                result.add(subFormula);
            }
        }

        return result;
    }

    /**
     * is not recurrent and is not produced by anything recurrent from f
     * currently, the latter is safely approximated by having a different
     * subformula
     */
    public boolean isTransientwrt(Formula f) {
        return !containsG() && isVeryDifferentFrom(f);
    }

    public abstract Formula not();

    // =============================================================
    // to be overridden by Boolean and Literal
    public Formula evaluateValuation(Set<String> valuation) {
        return this;
    }

    // to be overridden by Boolean and Literal
    public Formula evaluateLiteral(Literal literal) {
        return this;
    }

    // to be overridden by Boolean and Literal
    public Literal getAnUnguardedLiteral() {
        return null;
    }

    // to be overridden by Boolean and XOperator
    public Formula removeX() {
        return this;
    }

    // to be overridden by Boolean and GOperator
    public Formula substituteGsToFalse(GSet gSet) {
        return this;
    }

    // to be overridden by Boolean
    public Formula removeConstants() {
        return this;
    }

    // to be overridden by Boolean
    // contains a modal/Literal not contained in f
    public boolean isVeryDifferentFrom(Formula f) {
        return !f.hasSubformula(this);
    }

    // to be overridden by Boolean
    public boolean ignoresG(Formula f) {
        // return false;
        return !hasSubformula(f);
    }

    /**
     * This method removes boolean constants such as true/false also inside of
     * temporary operators,
     * 
     * @return Forumula, where all boolean constants are ommitted (if possible)
     */
    public abstract Formula rmAllConstants();

    /**
     * For the propositional view on LTL modal operators (F, G, U, X) and
     * literals (a, !a) are treated as propositions. The method reduces the set
     * by leaving out the negation of a formula. The propositional reasoning
     * libraries are expected to register negations accordingly.
     *
     * @return
     */
    public abstract Set<Formula> getPropositions();

    public abstract Set<String> getAtoms();

    public abstract <R> R accept(Visitor<R> v);

    public abstract <A, B> A accept(BinaryVisitor<A, B> v, B f);

    public abstract <A, B, C> A accept(TripleVisitor<A, B, C> v, B f, C c);

    // Temporal Properties of an LTL Formula
    public abstract boolean isPureEventual();

    public abstract boolean isPureUniversal();

    public abstract boolean isSuspendable();

}
