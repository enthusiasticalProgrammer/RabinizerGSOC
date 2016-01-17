package rabinizer.ltl.bdd;

import com.google.common.collect.Sets;
import net.sf.javabdd.BDD;
import org.jetbrains.annotations.NotNull;
import rabinizer.ltl.Formula;
import rabinizer.ltl.Literal;
import rabinizer.ltl.ValuationSet;

import java.util.*;

public class BDDValuationSet extends AbstractSet<Set<String>> implements ValuationSet {

    private final BDDValuationSetFactory factory;
    private BDD valuations;

    BDDValuationSet(BDD bdd, BDDValuationSetFactory factory) {
        this.valuations = bdd;
        this.factory = factory;
    }

    @Override
    public Formula toFormula() {
        return factory.createRepresentative(valuations);
    }

    @Override
    public Set<String> pickAny() {
        return factory.pickAny(valuations);
    }

    @Override
    public boolean restrictWith(Literal literal) {
        return update(valuations.andWith(factory.createBDD(literal)));
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof BDDValuationSet) {
            BDD otherBDD = ((BDDValuationSet) o).valuations;
            return otherBDD.equals(valuations);
        }

        return false;
    }

    @Override
    public boolean isUniverse() {
        return valuations.isOne();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.valuations);
    }

    @Override
    public boolean contains(Object o) {
        if (!(o instanceof Set)) {
            return false;
        }

        Set<String> valuation = (Set<String>) o;
        BDDValuationSet set = factory.createValuationSet(valuation);

        return !valuations.and(set.valuations).isZero();
    }

    @Override
    public @NotNull Iterator<Set<String>> iterator() {
        return Sets.powerSet(factory.getAlphabet()).stream().filter(this::contains).iterator();
    }

    @Override
    public int size() {
        return (int) Math.round(valuations.satCount());
    }

    @Override
    public boolean isEmpty() {
        return valuations.isZero();
    }

    @Override
    public boolean add(Set<String> v) {
        BDDValuationSet vs = factory.createValuationSet(v);
        return update(valuations.orWith(vs.valuations));
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends Set<String>> c) {
        if (c instanceof BDDValuationSet) {
            BDD otherValuations = ((BDDValuationSet) c).valuations;
            BDD newValuations = valuations.or(otherValuations);
            return update(newValuations);
        }

        return super.addAll(c);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        if (c instanceof BDDValuationSet) {
            BDD otherValuations = ((BDDValuationSet) c).valuations;
            BDD newValuations = valuations.and(otherValuations);
            return update(newValuations);
        }

        return super.retainAll(c);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        if (c instanceof BDDValuationSet) {
            BDD otherValuations = ((BDDValuationSet) c).valuations;
            BDD newValuations = valuations.and(otherValuations.not());
            return update(newValuations);
        }

        return super.removeAll(c);
    }

    @Override
    public ValuationSet complement() {
        return new BDDValuationSet(valuations.not(), factory);
    }

    @Override
    public BDDValuationSet clone() {
        return new BDDValuationSet(valuations.id(), factory);
    }

    @Override
    public String toString() {
        return Sets.newHashSet(iterator()).toString();
    }

    private boolean update(BDD newValue) {
        if (this.valuations.equals(newValue)) {
            return false;
        }

        this.valuations = newValue;
        return true;
    }
}
