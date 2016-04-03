/*
 * Copyright (C) 2016  (See AUTHORS)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package rabinizer.collections.valuationset;

import com.google.common.collect.Sets;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import rabinizer.ltl.*;
import rabinizer.ltl.simplifier.Simplifier;
import rabinizer.ltl.simplifier.Simplifier.Strategy;

import java.lang.reflect.Method;
import java.util.*;

public class BDDValuationSetFactory implements ValuationSetFactory {

    final BDDFactory factory;
    final String[] mapping;

    public BDDValuationSetFactory(Formula formula) {
        this(AlphabetVisitor.extractAlphabet(formula));
    }

    public BDDValuationSetFactory(Set<String> alphabet) {
        mapping = new String[alphabet.size()];
        alphabet.toArray(mapping);
        Arrays.sort(mapping);

        int size = Math.max(alphabet.size(), 1);

        factory = BDDFactory.init("micro", 64 * size, 1000);
        factory.setVarNum(size);

        // Silence library, TODO: move to logging util class
        try {
            Method m = BDDValuationSetFactory.class.getDeclaredMethod("callback", int.class, Object.class);
            factory.registerGCCallback(this, m);
            factory.registerReorderCallback(this, m);
            factory.registerResizeCallback(this, m);
        } catch (SecurityException | NoSuchMethodException e) {
            System.err.println("Failed to silence BDD library: " + e);
        }
    }

    @Override
    public Collection<String> getAlphabet() {
        return Collections.unmodifiableCollection(Arrays.asList(mapping));
    }

    @Override
    public BDDValuationSet createEmptyValuationSet() {
        return new BDDValuationSet(factory.zero());
    }

    @Override
    public BDDValuationSet createUniverseValuationSet() {
        return new BDDValuationSet(factory.one());
    }

    @Override
    public BDDValuationSet createValuationSet(Set<String> valuation) {
        return createValuationSet(valuation, getAlphabet());
    }

    @Override
    public BDDValuationSet createValuationSet(Set<String> valuation, Collection<String> base) {
        return new BDDValuationSet(createBDD(valuation, base));
    }

    public void callback(int x, Object stats) {

    }

    Formula createRepresentative(BDD bdd) {
        if (bdd.isOne()) {
            return BooleanConstant.TRUE;
        }

        if (bdd.isZero()) {
            return BooleanConstant.FALSE;
        }

        String letter = mapping[bdd.level()];

        Formula pos = createRepresentative(bdd.high());
        Formula neg = createRepresentative(bdd.low());

        return Simplifier.simplify(new Disjunction(new Conjunction(new Literal(letter, false), pos), new Conjunction(new Literal(letter, true), neg)), Strategy.PROPOSITIONAL);
    }

    BDD createBDD(String letter, boolean negate) {
        int i = Arrays.binarySearch(mapping, letter);

        if (i < 0) {
            throw new IllegalArgumentException("The alphabet does not contain the following letter: " + letter);
        }

        if (negate) {
            return factory.nithVar(i);
        }

        return factory.ithVar(i);
    }

    BDD createBDD(Set<String> set, Collection<String> base) {
        final BDD bdd = factory.one();
        base.forEach(letter -> bdd.andWith(createBDD(letter, !set.contains(letter))));
        return bdd;
    }

    boolean isSatAssignment(BDD valuations, Set<String> valuation) {
        BDD current = valuations;

        while (!current.isOne() && !current.isZero()) {
            int var = current.var();

            if (valuation.contains(mapping[var])) {
                current = current.high();
            } else {
                current = current.low();
            }
        }

        return current.isOne();
    }

    public class BDDValuationSet extends AbstractSet<Set<String>> implements ValuationSet {

        private BDD valuations;

        BDDValuationSet(BDD bdd) {
            valuations = bdd;
        }

        @Override
        public Formula toFormula() {
            return createRepresentative(valuations);
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
            return valuations.hashCode();
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Set)) {
                return false;
            }

            return isSatAssignment(valuations, (Set<String>) o);
        }

        @Override
        public Iterator<Set<String>> iterator() {
            return Sets.powerSet(new HashSet<>(getAlphabet())).stream().filter(this::contains).iterator();
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
            BDDValuationSet vs = createValuationSet(v);
            return update(vs.valuations.or(valuations));
        }

        @Override
        public boolean addAll(Collection<? extends Set<String>> c) {
            if (c instanceof BDDValuationSet) {
                BDD otherValuations = ((BDDValuationSet) c).valuations;
                BDD newValuations = valuations.or(otherValuations);
                return update(newValuations);
            }

            return super.addAll(c);
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            if (c instanceof BDDValuationSet) {
                BDD otherValuations = ((BDDValuationSet) c).valuations;
                BDD newValuations = valuations.and(otherValuations);
                return update(newValuations);
            }

            return super.retainAll(c);
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            if (c instanceof BDDValuationSet) {
                BDD otherValuations = ((BDDValuationSet) c).valuations;
                BDD newValuations = valuations.and(otherValuations.not());
                return update(newValuations);
            }

            return super.removeAll(c);
        }

        @Override
        public ValuationSet complement() {
            return new BDDValuationSet(valuations.not());
        }

        @Override
        public BDDValuationSet clone() {
            return new BDDValuationSet(valuations.id());
        }

        @Override
        public String toString() {
            return Sets.newHashSet(iterator()).toString();
        }

        private boolean update(BDD newValue) {
            if (valuations.equals(newValue)) {
                return false;
            }

            valuations = newValue;
            return true;
        }
    }
}
