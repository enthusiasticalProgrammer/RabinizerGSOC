package rabinizer.ltl;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import rabinizer.automata.GSet;
import rabinizer.ltl.bdd.BDDEquivalenceClassFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Jan Kretinsky
 */
public abstract class Formula {

    String cachedString;
    BoolExpr cachedLTL;
    private int cachedHashCode;

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

    public abstract BoolExpr toExpr(Context ctx);

    public abstract boolean hasSubformula(Formula f);

    public abstract Set<Formula> gSubformulas();

    public abstract Set<Formula> topmostGs();

    // unfold everything, used in master automaton
    public abstract Formula unfold();

    // unfold everything but G's, used in slave automata
    public abstract Formula unfoldNoG();

    public Formula temporalStep(Set<String> valuation) {
        return this.evaluate(valuation).removeX();
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

        result.addAll(candidates.stream().filter(subFormula -> hasSubformula(subFormula) && !clazz.getSimplifiedRepresentative().ignoresG(subFormula)).collect(Collectors.toList()));

        return result;
    }

    public abstract Formula not();

    // =============================================================
    // to be overridden by Boolean and Literal
    public Formula evaluate(Set<String> valuation) {
        return this;
    }

    // to be overridden by Boolean and Literal
    public Formula evaluate(Literal literal) {
        return this;
    }

    // to be overridden by Boolean and Literal
    public Optional<Literal> getAnUnguardedLiteral() {
        return Optional.empty();
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
    public boolean ignoresG(Formula f) {
        // return false;
        return !hasSubformula(f);
    }

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
