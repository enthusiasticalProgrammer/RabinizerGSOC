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

package rabinizer.ltl.equivalence;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import org.jetbrains.annotations.NotNull;
import rabinizer.collections.Tuple;
import rabinizer.ltl.*;
import rabinizer.ltl.simplifier.Simplifier;
import rabinizer.ltl.simplifier.Simplifier.Strategy;

import java.lang.reflect.Method;
import java.util.*;

public class BDDEquivalenceClassFactory implements EquivalenceClassFactory {

    final BDDFactory factory;
    final Map<Formula, Integer> mapping;
    final List<Formula> reverseMapping;
    final Visitor<BDD> visitor;

    final LoadingCache<EquivalenceClass, EquivalenceClass> unfoldCache;
    final LoadingCache<EquivalenceClass, EquivalenceClass> unfoldGCache;
    final LoadingCache<Tuple<EquivalenceClass, Set<String>>, EquivalenceClass> temporalStepCache;

    public BDDEquivalenceClassFactory(Formula formula) {
        mapping = PropositionVisitor.extractPropositions(formula);
        reverseMapping = new ArrayList<>(mapping.size());

        int size = mapping.isEmpty() ? 1 : mapping.size();

        factory = BDDFactory.init("micro", 64 * size, 1000);
        factory.setVarNum(size);

        // Silence library
        try {
            Method m = BDDEquivalenceClassFactory.class.getDeclaredMethod("callback", int.class, Object.class);
            factory.registerGCCallback(this, m);
            factory.registerReorderCallback(this, m);
            factory.registerResizeCallback(this, m);
        } catch (SecurityException | NoSuchMethodException e) {
            System.err.println("Failed to silence BDD library: " + e);
        }

        visitor = new BDDVisitor();
        int var = 0;

        for (Map.Entry<Formula, Integer> entry : mapping.entrySet()) {
            reverseMapping.add(entry.getKey());
            entry.setValue(var);
            var++;
        }

        CacheLoader<EquivalenceClass, EquivalenceClass> unfoldLoader = new CacheLoader<EquivalenceClass, EquivalenceClass>() {
            @Override
            public EquivalenceClass load(EquivalenceClass key) throws Exception {
                return createEquivalenceClass(key.getRepresentative().unfold(false));
            }
        };

        CacheLoader<EquivalenceClass, EquivalenceClass> unfoldGLoader = new CacheLoader<EquivalenceClass, EquivalenceClass>() {
            @Override
            public EquivalenceClass load(EquivalenceClass key) throws Exception {
                return createEquivalenceClass(key.getRepresentative().unfold(true));
            }
        };

        CacheLoader<Tuple<EquivalenceClass, Set<String>>, EquivalenceClass> temporalStepLoader = new CacheLoader<Tuple<EquivalenceClass, Set<String>>, EquivalenceClass>() {
            @Override
            public EquivalenceClass load(Tuple<EquivalenceClass, Set<String>> arg) throws Exception {
                return createEquivalenceClass(arg.left.getRepresentative().temporalStep(arg.right));
            }
        };

        unfoldCache = CacheBuilder.newBuilder().build(unfoldLoader);
        unfoldGCache = CacheBuilder.newBuilder().build(unfoldGLoader);
        temporalStepCache = CacheBuilder.newBuilder().build(temporalStepLoader);
    }

    @Override
    public @NotNull EquivalenceClass getTrue() {
        return new BDDEquivalenceClass(BooleanConstant.TRUE, factory.one());
    }

    @Override
    public @NotNull EquivalenceClass getFalse() {
        return new BDDEquivalenceClass(BooleanConstant.FALSE, factory.zero());
    }

    @Override
    public @NotNull BDDEquivalenceClass createEquivalenceClass(@NotNull Formula formula) {
        Formula simplifiedFormula = Simplifier.simplify(formula, Strategy.PROPOSITIONAL);
        return new BDDEquivalenceClass(simplifiedFormula, createBDD(simplifiedFormula));
    }

    public void callback(int x, Object stats) {

    }

    BDD createBDD(Formula formula) {
        return formula.accept(visitor);
    }

    private class BDDVisitor implements Visitor<BDD> {
        @Override
        public BDD visit(@NotNull BooleanConstant b) {
            return b.value ? factory.one() : factory.zero();
        }

        @Override
        public BDD visit(@NotNull Conjunction c) {
            if (c.children.contains(BooleanConstant.FALSE)) {
                return factory.zero();
            }

            return c.children.stream().map(x -> x.accept(this)).reduce(factory.one(), BDD::andWith);
        }

        @Override
        public BDD visit(@NotNull Disjunction d) {
            if (d.children.contains(BooleanConstant.TRUE)) {
                return factory.one();
            }

            return d.children.stream().map(x -> x.accept(this)).reduce(factory.zero(), BDD::orWith);
        }

        @Override
        public BDD defaultAction(Formula formula) {
            Integer value = mapping.get(formula);

            if (value == null) {
                value = factory.extVarNum(1);
                reverseMapping.add(formula);
                mapping.put(formula, value);
            }

            return factory.ithVar(value);
        }
    }

    public class BDDEquivalenceClass implements EquivalenceClass {

        private final @NotNull BDD bdd;
        private final @NotNull Formula representative;

        BDDEquivalenceClass(@NotNull Formula representative, @NotNull BDD bdd) {
            this.representative = representative;
            this.bdd = bdd;
        }

        @Override
        public @NotNull Formula getRepresentative() {
            return representative;
        }

        @Override
        public boolean implies(@NotNull EquivalenceClass equivalenceClass) {
            if (!(equivalenceClass instanceof BDDEquivalenceClass)) {
                return false;
            }

            BDDEquivalenceClass that = (BDDEquivalenceClass) equivalenceClass;

            if (!bdd.getFactory().equals(that.bdd.getFactory())) {
                return false;
            }

            return bdd.imp(that.bdd).isOne();
        }

        @Override
        public boolean equivalent(EquivalenceClass equivalenceClass) {
            return equals(equivalenceClass);
        }

        @Override
        public @NotNull EquivalenceClass unfold(boolean unfoldG) {
            LoadingCache<EquivalenceClass, EquivalenceClass> cache = unfoldG ? unfoldGCache : unfoldCache;
            return cache.getUnchecked(this);
        }

        @Override
        public @NotNull EquivalenceClass temporalStep(Set<String> valuation) {
            return temporalStepCache.getUnchecked(new Tuple<>(this, valuation));
        }

        @Override
        public @NotNull EquivalenceClass and(@NotNull EquivalenceClass eq) {
            if (eq instanceof BDDEquivalenceClass) {
                return new BDDEquivalenceClass(Simplifier.simplify(new Conjunction(representative, eq.getRepresentative()), Strategy.PROPOSITIONAL), bdd.and(((BDDEquivalenceClassFactory.BDDEquivalenceClass) eq).bdd));
            }

            return createEquivalenceClass(new Conjunction(representative, eq.getRepresentative()));
        }

        @Override
        public @NotNull EquivalenceClass or(@NotNull EquivalenceClass eq) {
            if (eq instanceof BDDEquivalenceClass) {
                return new BDDEquivalenceClass(Simplifier.simplify(new Disjunction(representative, eq.getRepresentative()), Strategy.PROPOSITIONAL), bdd.or(((BDDEquivalenceClassFactory.BDDEquivalenceClass) eq).bdd));
            }

            return createEquivalenceClass(new Disjunction(representative, eq.getRepresentative()));
        }

        @Override
        public boolean isTrue() {
            return bdd.isOne();
        }

        @Override
        public boolean isFalse() {
            return bdd.isZero();
        }

        @Override
        public @NotNull Set<Formula> getSupport() {
            Set<Formula> support = new HashSet<>();
            getSupport(bdd, support);
            return support;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BDDEquivalenceClass that = (BDDEquivalenceClass) o;
            return Objects.equals(bdd, that.bdd) && Objects.equals(bdd.getFactory(), that.bdd.getFactory());
        }

        @Override
        public int hashCode() {
            return bdd.hashCode();
        }

        // We are not using bdd.support since it causes several NPE. Patch available on github/javabdd.
        private void getSupport(BDD bdd, Set<Formula> support) {
            if (bdd.isZero() || bdd.isOne()) {
                return;
            }

            support.add(reverseMapping.get(bdd.level()));
            getSupport(bdd.high(), support);
            getSupport(bdd.low(), support);
        }
    }
}
