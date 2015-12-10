package rabinizer.ltl.bdd;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import rabinizer.ltl.*;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

class BDDLibraryWrapper<K extends Formula> {
    protected final BDDFactory factory;
    protected final BiMap<K, BDD> mapping;

    private final BDDVisitor visitor;

    protected BDDLibraryWrapper(Set<K> domain) {
        factory = BDDFactory.init("java", 16 * domain.size(), 1000);
        factory.setVarNum(domain.size());

        ImmutableBiMap.Builder<K, BDD> builder = new ImmutableBiMap.Builder<>();

        int var = 0;
        for (K proposition : domain) {
            BDD pos = factory.ithVar(var);
            BDD neg = factory.nithVar(var);

            builder.put(proposition, pos);
            builder.put((K) proposition.not(), neg);

            var++;
        }

        mapping = builder.build();
        visitor = new BDDVisitor();
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

        return new Disjunction(new Conjunction(varpos, pos), new Conjunction(varneg, neg));
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
            if (c.getChildren().contains(BooleanConstant.FALSE)) {
                return factory.zero();
            }

            return c.getChildren().stream().map(x -> x.accept(this)).reduce(factory.one(), BDD::andWith);
        }

        @Override
        public BDD visit(Disjunction d) {
            if (d.getChildren().contains(BooleanConstant.TRUE)) {
                return factory.one();
            }

            return d.getChildren().stream().map(x -> x.accept(this)).reduce(factory.zero(), BDD::orWith);
        }

        @Override
        public BDD defaultAction(Formula f) {
            return mapping.get(f).id();
        }
    }
}
