package rabinizer.ltl.z3;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import com.google.common.math.IntMath;
import com.microsoft.z3.BoolExpr;

import rabinizer.ltl.Formula;
import rabinizer.ltl.FormulaFactory;
import rabinizer.ltl.Literal;
import rabinizer.ltl.ValuationSet;

public class Z3ValuationSet extends AbstractSet<Set<String>> implements ValuationSet {
    private final Z3ValuationSetFactory factory;
    private BoolExpr valuation;

    public Z3ValuationSet(Z3ValuationSetFactory f, BoolExpr val) {
        factory = f;
        valuation = val;
    }

    @Override
    public boolean add(Set<String> e) {
        Z3ValuationSet vs = factory.createValuationSet(e);
        return update(factory.or(this.valuation, vs.valuation));
    }

    @Override
    public boolean addAll(Collection<? extends Set<String>> c) {
        if (c instanceof Z3ValuationSet) {
            return update(factory.or(((Z3ValuationSet) c).valuation, this.valuation));
        }

        return super.addAll(c);
    }

    @Override
    public void clear() {
        super.clear();

    }

    @Override
    public boolean contains(Object o) {
        if (!(o instanceof Set)) {
            return false;
        }

        Set<String> val = (Set<String>) o;
        Z3ValuationSet set = factory.createValuationSet(val);

        return factory.checkImplies(set.valuation, this.valuation);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return c.stream().allMatch(s -> this.contains(s));
    }

    @Override
    public boolean isEmpty() {
        return factory.isContradiction(valuation);
    }

    @Override
    public Iterator<Set<String>> iterator() {
        return Sets.powerSet(factory.getAlphabet()).stream().filter(this::contains).iterator();
    }

    @Override
    public boolean remove(Object o) {
        if (o instanceof Set) {
            Z3ValuationSet vs = factory.createValuationSet((Set<String>) o);
            return update(factory.and(this.valuation, factory.negate(vs.valuation)));
        }
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return c.stream().anyMatch(s -> remove(s));
    }

    @Override
    public boolean retainAll(Collection<?> c) {

        if (c instanceof Z3ValuationSet) {
            BoolExpr otherValuations = ((Z3ValuationSet) c).valuation;
            BoolExpr newValuations = factory.and(valuation, otherValuations);
            return update(newValuations);
        }

        return super.retainAll(c);

    }

    @Override
    public ValuationSet complement() {
        return new Z3ValuationSet(factory, factory.negate(valuation));
    }

    @Override
    public boolean isUniverse() {
        return factory.isTautology(this.valuation);
    }

    @Override
    public boolean restrictWith(Literal literal) {
        return update(factory.and(valuation, factory.createZ3(literal)));
    }

    @Override
    public Set<String> pickAny() {
        return factory.pickAny(valuation);
    }

    @Override
    public Formula toFormula() {
        return factory.createRepresentative(valuation);
    }

    @Override
    public Z3ValuationSet clone() {
        return new Z3ValuationSet(factory, valuation);
    }

    private boolean update(BoolExpr or) {
        if (!factory.checkEquality(valuation, or)) {
            this.valuation = or;
            return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Z3ValuationSet) {
            BoolExpr otherZ3 = ((Z3ValuationSet) o).valuation;
            return otherZ3.equals(valuation);
        }
        return false;
    }

    /**
     * This method would solve an NP-hard Problem--> it is not implemented
     */
    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.valuation);
    }

}
