package rabinizer.ltl.bdd;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import rabinizer.ltl.*;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

public abstract class BDDLibraryWrapper<K extends Formula> {
    protected final BDDFactory factory;
    protected final BiMap<K, BDD> mapping;

    private final BDDVisitor visitor;

    protected BDDLibraryWrapper(Set<K> domain) {
        final int size = domain.size() > 0 ? domain.size() : 1;

        factory = BDDFactory.init("java", 64 * size, 1000);
        factory.setVarNum(size);

        // Silence library
        try {
            Method m = BDDLibraryWrapper.class.getDeclaredMethod("callback", new Class[]{int.class, Object.class});
            factory.registerGCCallback(this, m);
            factory.registerReorderCallback(this, m);
            factory.registerResizeCallback(this, m);
        } catch (SecurityException | NoSuchMethodException e) {
            System.err.println("Failed to silence BDD library: " + e);
        }

        mapping = HashBiMap.create(domain.size());
        visitor = new BDDVisitor();

        int var = 0;

        for (K proposition : domain) {
            BDD pos = factory.ithVar(var);
            BDD neg = factory.nithVar(var);

            mapping.put(proposition, pos);
            mapping.put((K) proposition.not(), neg);

            var++;
        }
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

        BDD var = factory.ithVar(bdd.level());

        Formula varpos = mapping.inverse().get(var);
        Formula varneg = mapping.inverse().get(var.not());

        Formula pos = createRepresentative(bdd.high());
        Formula neg = createRepresentative(bdd.low());

        return Simplifier.simplify(new Disjunction(new Conjunction(varpos, pos), new Conjunction(varneg, neg)), Simplifier.Strategy.PROPOSITIONAL);
    }

    BDD createBDD(Formula formula) {
        return formula.accept(visitor);
    }

    Set<K> getSatAssignment(BDD bdd) {
        if (bdd.isZero()) {
            throw new NoSuchElementException();
        }

        if (bdd.isOne()) {
            return new HashSet<>();
        }

        Set<K> result;
        BDD var;

        if (bdd.high().isZero()) {
            var = factory.nithVar(bdd.level());
            result = getSatAssignment(bdd.low());
        } else {
            var = factory.ithVar(bdd.level());
            result = getSatAssignment(bdd.high());
        }

        K key = mapping.inverse().get(var);

        result.add(key);
        return result;
    }

    private class BDDVisitor implements Visitor<BDD> {
        @Override
        public BDD visit(BooleanConstant b) {
            return b.value ? factory.one() : factory.zero();
        }

        @Override
        public BDD visit(Conjunction c) {
            if (c.children.contains(BooleanConstant.FALSE)) {
                return factory.zero();
            }

            return c.children.stream().map(x -> x.accept(this)).reduce(factory.one(), BDD::andWith);
        }

        @Override
        public BDD visit(Disjunction d) {
            if (d.children.contains(BooleanConstant.TRUE)) {
                return factory.one();
            }

            return d.children.stream().map(x -> x.accept(this)).reduce(factory.zero(), BDD::orWith);
        }

        @Override
        public BDD defaultAction(Formula formula) {
            BDD value = mapping.get(formula);

            if (value == null) {
                int var = factory.extVarNum(1);

                mapping.put((K) formula, factory.ithVar(var));
                mapping.put((K) formula.not(), factory.nithVar(var));

                return factory.ithVar(var);
            }

            return value.id();
        }
    }
}
