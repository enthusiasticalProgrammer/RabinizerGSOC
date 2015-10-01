package rabinizer.ltl;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import net.sf.javabdd.BDD;
import rabinizer.ltl.bdd.BDDForFormulae;
import rabinizer.ltl.bdd.GSet;
import rabinizer.ltl.bdd.Valuation;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Jan Kretinsky
 */
public abstract class Formula {

    String cachedString;
    BDD cachedBdd = null;
    BoolExpr cachedLTL = null;
    private int cachedHashCode = -1;

    public abstract BDD bdd();

    public Formula representative() {
        return BDDForFormulae.representativeOfBdd(bdd(), this);
    }

    @Override
    public int hashCode() {
        if (cachedHashCode == -1) {
            cachedHashCode = hashCodeOnce();
        }

        return cachedHashCode;
    }

    protected abstract int hashCodeOnce();

    @Override
    public abstract boolean equals(Object o);

    // to be overwritten, side-effects: propositions will be inserted into ctx
    public abstract BoolExpr toExpr(Context ctx);

    public abstract boolean containsG();

    public abstract boolean hasSubformula(Formula f);

    public abstract Set<Formula> gSubformulas();

    public abstract Set<Formula> topmostGs();

    // unfold everything, used in master automaton
    public abstract Formula unfold();

    // unfold everything but G's, used in slave automata
    public abstract Formula unfoldNoG();

    public Formula temporalStep(Valuation valuation) {
        return this.assertValuation(valuation).removeX();
    }

    public Formula assertValuation(Valuation valuation) {
        return evaluateValuation(valuation).removeConstants();
    }

    public Formula assertLiteral(Literal literal) {
        return evaluateLiteral(literal).removeConstants();
    }

    public Set<Formula> relevantGFormulas(Set<Formula> candidates) { // TODO: is
                                                                     // with
                                                                     // the
                                                                     // outer
                                                                     // G
                                                                     // (not
                                                                     // GSet)
        Set<Formula> result = new HashSet<>();
        for (Formula subFormula : candidates) {
            if (hasSubformula(subFormula) && !unfold().representative().ignoresG(subFormula)) {
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
    public Formula evaluateValuation(Valuation valuation) {
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

    public abstract <R> R accept(Visitor<R> v);

    public abstract <A, B> A accept(BinaryVisitor<A, B> v, B f);

    public abstract <A, B, C> A accept(TripleVisitor<A, B, C> v, B f, C c);

    // Temporal Properties of an LTL Formula
    public abstract boolean isPureEventual();

    public abstract boolean isPureUniversal();

    public abstract boolean isSuspendable();

}
