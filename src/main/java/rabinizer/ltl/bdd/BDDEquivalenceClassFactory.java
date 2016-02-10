package rabinizer.ltl.bdd;

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
import rabinizer.ltl.Simplifier.Strategy;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class BDDEquivalenceClassFactory implements EquivalenceClassFactory {

    final BDDFactory factory;
    final BiMap<Formula, BDD> mapping;
    final Visitor<BDD> visitor;

    final LoadingCache<EquivalenceClass, EquivalenceClass> unfoldCache;
    final LoadingCache<EquivalenceClass, EquivalenceClass> unfoldGCache;
    final LoadingCache<Tuple<EquivalenceClass, Set<String>>, EquivalenceClass> temporalStepCache;

    public BDDEquivalenceClassFactory(Set<Formula> domain) {
        int size = domain.isEmpty() ? 1 : domain.size();

        factory = BDDFactory.init("java", 64 * size, 1000);
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

        mapping = HashBiMap.create(size);
        visitor = new BDDVisitor();

        int var = 0;

        for (Formula proposition : domain) {
            BDD pos = factory.ithVar(var);
            mapping.put(proposition, pos);
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
        public BDD defaultAction(@NotNull Formula formula) {
            BDD value = mapping.get(formula);

            if (value == null) {
                value = factory.ithVar(factory.extVarNum(1));
                mapping.put(formula, value);
            }

            return value.id();
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
        public EquivalenceClass unfold(boolean unfoldG) {
            LoadingCache<EquivalenceClass, EquivalenceClass> cache = unfoldG ? unfoldGCache : unfoldCache;
            return cache.getUnchecked(this);
        }

        @Override
        public EquivalenceClass temporalStep(Set<String> valuation) {
            return temporalStepCache.getUnchecked(new Tuple<>(this, valuation));
        }

        @Override
        public @NotNull EquivalenceClass and(@NotNull EquivalenceClass eq) {
            if (eq instanceof BDDEquivalenceClass) {
                return new BDDEquivalenceClass(Simplifier.simplify(new Conjunction(getRepresentative(), eq.getRepresentative()), Strategy.PROPOSITIONAL), bdd.and(((rabinizer.ltl.bdd.BDDEquivalenceClassFactory.BDDEquivalenceClass) eq).bdd));
            }

            return createEquivalenceClass(new Conjunction(getRepresentative(), eq.getRepresentative()));
        }

        @Override
        public @NotNull EquivalenceClass or(@NotNull EquivalenceClass eq) {
            if (eq instanceof BDDEquivalenceClass) {
                return new BDDEquivalenceClass(Simplifier.simplify(new Disjunction(getRepresentative(), eq.getRepresentative()), Strategy.PROPOSITIONAL), bdd.or(((rabinizer.ltl.bdd.BDDEquivalenceClassFactory.BDDEquivalenceClass) eq).bdd));
            }

            return createEquivalenceClass(new Disjunction(getRepresentative(), eq.getRepresentative()));
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
        public Set<Formula> getSupport() {
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

        // We are not using bdd.support since it causes several NPE.
        private void getSupport(BDD bdd, Set<Formula> support) {
            if (bdd.isZero() || bdd.isOne()) {
                return;
            }

            BDD var = factory.ithVar(bdd.level());
            support.add(mapping.inverse().get(var));

            getSupport(bdd.high(), support);
            getSupport(bdd.low(), support);
        }
    }
}
