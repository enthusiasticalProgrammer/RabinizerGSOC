package rabinizer.automata;

import rabinizer.ltl.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public abstract class FormulaAutomatonState {

    final protected EquivalenceClass clazz;

    public FormulaAutomatonState(EquivalenceClass clazz) {
        this.clazz = clazz;
    }

    @Override
    public String toString() {
        return Simplifier.simplify(clazz.getRepresentative(), Simplifier.Strategy.PROPOSITIONAL).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FormulaAutomatonState that = (FormulaAutomatonState) o;
        return Objects.equals(this.getOuter(), that.getOuter()) && Objects.equals(clazz, that.clazz);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clazz);
    }

    public EquivalenceClass getClazz() {
        return clazz;
    }

    protected abstract Object getOuter();

    protected abstract ValuationSet createUniverseValuationSet();

    protected Set<ValuationSet> generatePartitioning(Formula f) {
        Set<ValuationSet> result = new HashSet<>();
        Optional<Literal> l = f.getAnUnguardedLiteral();

        if (!l.isPresent()) {
            result.add(createUniverseValuationSet());
        } else {
            Literal literal = l.get().positiveLiteral();
            Set<ValuationSet> pos = generatePartitioning(f.evaluate(literal));
            Set<ValuationSet> neg = generatePartitioning(f.evaluate(literal.not()));
            for (ValuationSet vs : pos) {
                vs.restrictWith(literal);
                result.add(vs);
            }
            for (ValuationSet vs : neg) {
                vs.restrictWith(literal.not());
                result.add(vs);
            }
        }

        return result;
    }
}

