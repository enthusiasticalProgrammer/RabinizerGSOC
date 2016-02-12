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
