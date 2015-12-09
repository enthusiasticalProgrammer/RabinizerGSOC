package rabinizer.automata;

import rabinizer.ltl.EquivalenceClass;
import rabinizer.ltl.Formula;
import rabinizer.ltl.SimplifyBooleanVisitor;

import java.util.Objects;

public class FormulaAutomatonState {

    final private EquivalenceClass clazz;

    public FormulaAutomatonState(EquivalenceClass clazz) {
        this.clazz = clazz;
    }

    public EquivalenceClass getEquivalenceClass() {
        return clazz;
    }

    public Formula getFormula() {
        return SimplifyBooleanVisitor.simplify(clazz.getRepresentative());
    }

    @Override
    public String toString() {
        return getFormula().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FormulaAutomatonState)) {
            return false;
        } else {
            return clazz.equals(((FormulaAutomatonState) o).clazz);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(clazz);
    }
}
