package rabinizer.ltl.bdd;

import net.sf.javabdd.BDD;
import rabinizer.ltl.Formula;
import rabinizer.ltl.Literal;

import java.util.Set;

public abstract class ValuationSet {

    // protected static BDDFactory bf;
    public abstract Formula toFormula();

    public abstract BDD toBdd();

    public abstract Set<Valuation> toSet();

    public abstract Valuation pickAny();

    public abstract ValuationSet add(ValuationSet vs);

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

    public abstract boolean isAllVals();

    public abstract boolean contains(Valuation v);

    public abstract boolean contains(ValuationSet vs);

    public abstract boolean isEmpty();

    public abstract ValuationSet and(Literal literal);

    public abstract ValuationSet and(ValuationSet vs);

    public abstract ValuationSet or(ValuationSet vs);

    public abstract ValuationSet complement();

    public abstract void remove(ValuationSet vs);

    @Override
    public String toString() {
        return this.toFormula().toString();
    }

}
