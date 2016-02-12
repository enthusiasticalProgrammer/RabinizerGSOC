package rabinizer.automata;

import rabinizer.ltl.equivalence.EquivalenceClass;
import rabinizer.ltl.equivalence.EquivalenceClassFactory;
import rabinizer.ltl.*;
import rabinizer.collections.valuationset.ValuationSetFactory;

import java.util.*;

public class AccLocalFolded extends AccLocal {

    public AccLocalFolded(Product product, ValuationSetFactory factory, EquivalenceClassFactory factory2, Collection<Optimisation> opts) {
        super(product, factory, factory2, opts);
    }

    @Override
    protected boolean slavesEntail(Product.ProductState ps, Map<GOperator, Integer> ranking, Set<String> v,
                                   EquivalenceClass consequent) {
        Set<GOperator> gSet = ranking.keySet();
        Collection<Formula> children = new ArrayList<>(2 * gSet.size());

        for (GOperator f : gSet) {
            children.add(f);
            Formula slaveAntecedent = BooleanConstant.get(true);

            if (ps.getSecondaryState(f) != null) {
                RabinSlave.State rs = ps.getSecondaryState(f);

                for (Map.Entry<MojmirSlave.State, Integer> entry : rs.entrySet()) {
                    if (entry.getValue() >= ranking.get(f)) {
                        slaveAntecedent = new Conjunction(slaveAntecedent, entry.getKey().getClazz().getRepresentative());
                    }
                }
            }

            slaveAntecedent = slaveAntecedent.evaluate(gSet);
            children.add(slaveAntecedent);
        }

        EquivalenceClass antClazz = equivalenceClassFactory.createEquivalenceClass(new Conjunction(children));
        return antClazz.implies(consequent);
    }

    @Override
    protected TranSet<Product.ProductState> computeAccMasterForState(Map<GOperator, Integer> ranking, Product.ProductState ps) {
        TranSet<Product.ProductState> result = new TranSet<>(valuationSetFactory);

        if (!slavesEntail(ps, ranking, null, ps.getPrimaryState().getClazz())) {
            result.add(ps, valuationSetFactory.createUniverseValuationSet());
        }

        return result;
    }

}
