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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import ltl.FrequencyG;
import ltl.GOperator;
import ltl.UnaryModalOperator;
import ltl.equivalence.EquivalenceClassFactory;
import omega_automaton.collections.TranSet;
import omega_automaton.collections.valuationset.ValuationSetFactory;
import rabinizer.frequencyLTL.FOperatorForMojmir;

public class AccLocalControllerSynthesis extends
AccLocal<Set<UnaryModalOperator>, TranSet<Product<FrequencySelfProductSlave.State>.ProductState>, Map<TranSet<Product<FrequencySelfProductSlave.State>.ProductState>, Integer>, FrequencySelfProductSlave.State, ProductControllerSynthesis> {

    public AccLocalControllerSynthesis(ProductControllerSynthesis product, ValuationSetFactory valuationSetFactory, EquivalenceClassFactory equivalenceFactory,
            Collection<Optimisation> opts) {
        super(product, valuationSetFactory, equivalenceFactory, opts);
    }

    @Override
    protected void computeAccMasterForASingleGSet(Set<UnaryModalOperator> gSet,
            Map<Set<UnaryModalOperator>, TranSet<Product<FrequencySelfProductSlave.State>.ProductState>> result) {

        TranSet<Product<FrequencySelfProductSlave.State>.ProductState> avoidP = new TranSet<>(valuationSetFactory);

        for (Product<rabinizer.automata.FrequencySelfProductSlave.State>.ProductState ps : product.getStates()) {
            avoidP.addAll(computeNonAccMasterTransForStateIgoringRankings(gSet, ps));
        }

        if (!product.containsAllTransitions(avoidP)) {
            result.put(ImmutableSet.copyOf(gSet), avoidP);
        }
    }

    @Override
    protected Map<TranSet<Product<FrequencySelfProductSlave.State>.ProductState>, Integer> getSingleSlaveAccCond(UnaryModalOperator g, Set<MojmirSlave.State> finalStates) {
        if (g instanceof FrequencyG) {
            return product.getControllerAcceptanceFrequencyG((FrequencyG) g, finalStates);
        } else if (g instanceof GOperator) {
            Map<TranSet<Product<FrequencySelfProductSlave.State>.ProductState>, Integer> result = new HashMap<>();
            result.put(product.getControllerAcceptanceG((GOperator) g, finalStates), 0);
            return result;
        } else if (g instanceof FOperatorForMojmir) {
            Map<TranSet<Product<FrequencySelfProductSlave.State>.ProductState>, Integer> result = new HashMap<>();
            result.put(product.getControllerAcceptanceF((FOperatorForMojmir) g, finalStates), 0);
            return result;
        }
        throw new IllegalArgumentException("Formula is not a valid label of slave automata.");
    }
}
