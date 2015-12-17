package rabinizer.automata;

import rabinizer.ltl.*;

import java.util.Map;
import java.util.Set;

/**
 * @author jkretinsky
 */
public class AccLocalFolded extends AccLocal {

    public AccLocalFolded(Product product, ValuationSetFactory<String> factory, EquivalenceClassFactory factory2) {
        super(product, factory, factory2);
    }

    @Override
    protected boolean slavesEntail(Set<GOperator> gSet, Product.ProductState ps, Map<Formula, Integer> ranking, Set<String> v,
                                   EquivalenceClass consequent) {
        Formula antecedent = BooleanConstant.get(true);
        for (GOperator f : gSet) {
            antecedent = FormulaFactory.mkAnd(antecedent, f); // TODO relevant
            // for Folded
            // version
            // antecedent = new Conjunction(antecedent, new XOperator(new
            // GOperator(f))); // TODO:remove; relevant for Xunfolding
            Formula slaveAntecedent = BooleanConstant.get(true);
            if (ps.getSecondaryState(f) != null) {
                RabinSlave.State rs = ps.getSecondaryState(f);

                for (Map.Entry<MojmirSlave.State, Integer> entry : rs.entrySet()) {
                    if (entry.getValue() >= ranking.get(f)) {
                        slaveAntecedent = FormulaFactory.mkAnd(slaveAntecedent, entry.getKey().getClazz().getRepresentative());
                    }
                }
            }
            slaveAntecedent = slaveAntecedent.evaluate(gSet);
            antecedent = FormulaFactory.mkAnd(antecedent, slaveAntecedent);
        }
        return entails(antecedent, consequent);
    }

    @Override
    protected TranSet computeAccMasterForState(Set<GOperator> gSet, Map<Formula, Integer> ranking, Product.ProductState ps) {
        TranSet result = new TranSet(valuationSetFactory);
        if (!slavesEntail(gSet, ps, ranking, null, (ps.getPrimaryState()).getClazz())) {
            result.add(ps, valuationSetFactory.createUniverseValuationSet());
        }
        return result;
    }

}
