package rabinizer.ltl.bdd;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import rabinizer.collections.Tuple;
import rabinizer.ltl.BooleanConstant;
import rabinizer.ltl.EquivalenceClass;
import rabinizer.ltl.EquivalenceClassFactory;
import rabinizer.ltl.Formula;

import java.util.Set;

public class BDDEquivalenceClassFactory extends BDDLibraryWrapper<Formula> implements EquivalenceClassFactory {

    private final LoadingCache<EquivalenceClass, EquivalenceClass> unfoldCache;
    private final LoadingCache<EquivalenceClass, EquivalenceClass> unfoldGCache;
    private final LoadingCache<Tuple<EquivalenceClass, Set<String>>, EquivalenceClass> temporalStepCache;

    public BDDEquivalenceClassFactory(Set<Formula> domain) {
        super(domain);

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
    public EquivalenceClass getTrue() {
        return new BDDEquivalenceClass(BooleanConstant.TRUE, factory.one(), this);
    }

    @Override
    public EquivalenceClass getFalse() {
        return new BDDEquivalenceClass(BooleanConstant.FALSE, factory.zero(), this);
    }

    @Override
    public BDDEquivalenceClass createEquivalenceClass(Formula formula) {
        return new BDDEquivalenceClass(formula, createBDD(formula), this);
    }

    EquivalenceClass temporalStep(BDDEquivalenceClass clazz, Set<String> valuation) {
        return temporalStepCache.getUnchecked(new Tuple<>(clazz, valuation));
    }

    EquivalenceClass unfold(final BDDEquivalenceClass clazz, final boolean unfoldG) {
        LoadingCache<EquivalenceClass, EquivalenceClass> cache = unfoldG ? unfoldGCache : unfoldCache;
        return cache.getUnchecked(clazz);
    }

}
