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

import omega_automaton.Automaton;
import omega_automaton.AutomatonState;
import omega_automaton.acceptance.RabinAcceptance;
import omega_automaton.collections.TranSet;
import omega_automaton.collections.Tuple;
import omega_automaton.collections.valuationset.ValuationSet;
import omega_automaton.collections.valuationset.ValuationSetFactory;
import omega_automaton.output.HOAConsumerExtended;
import omega_automaton.output.HOAConsumerGeneralisedRabin;
import rabinizer.automata.Product.ProductState;

import javax.annotation.Nullable;

import com.google.common.collect.BiMap;

import jhoafparser.consumer.HOAConsumer;

import java.util.*;
import java.util.stream.Collectors;

public class DTRA extends Automaton<DTRA.ProductDegenState, RabinAcceptance<DTRA.ProductDegenState>> {

    private final DTGRA dtgra;

    public DTRA(DTGRA dtgra) {
        super(dtgra.valuationSetFactory);
        this.dtgra = dtgra;
        generate();

        int i = 0;
        for (Tuple<TranSet<ProductState>, List<TranSet<ProductState>>> grp : dtgra.acceptance.acceptanceCondition) {
            TranSet<ProductDegenState> fin = new TranSet<>(valuationSetFactory);
            TranSet<ProductDegenState> inf = new TranSet<>(valuationSetFactory);

            for (ProductDegenState s : getStates()) {
                ValuationSet vsFin = grp.left.asMap().get(s.productState);
                if (vsFin != null) {
                    fin.addAll(s, vsFin);
                }

                if (s.awaitedIndices[i] == grp.infs.size()) {
                    inf.addAll(s, valuationSetFactory.createUniverseValuationSet());
                }
            }
            i++;

            acceptance.addRabinPair(fin, inf);
        }
    }

    @Override
    protected HOAConsumerExtended getHOAConsumer(HOAConsumer ho, BiMap<String, Integer> aliases) {
        return new HOAConsumerGeneralisedRabin(ho, valuationSetFactory, aliases, initialState, acceptance, size());
    }

    @Override
    protected ProductDegenState generateInitialState() {
        return new ProductDegenState(dtgra.getInitialState(), new int[dtgra.acceptance.acceptanceCondition.size()]);
    }

    public class ProductDegenState implements AutomatonState<ProductDegenState> {

        public final Product.ProductState productState;
        public final int[] awaitedIndices;

        public ProductDegenState(Product.ProductState ps, int... awaitedIndices) {
            this.productState = ps;
            this.awaitedIndices = awaitedIndices;
        }

        @Override
        public String toString() {
            return productState + " " + Arrays.toString(awaitedIndices);
        }

        @Nullable
        @Override
        public Edge<ProductDegenState> getSuccessor(BitSet valuation) {
            Edge<ProductState<?>> successor = dtgra.getSuccessor(productState, valuation);

            if (successor == null) {
                return null;
            }

            GeneralisedRabinAcceptance<ProductState<?>> acc = dtgra.getAcceptance();

            int[] awaitedIndices = new int[acc.acceptanceCondition.size()];

            // TODO: Use listIterator
            int i = 0;
            for (Tuple<TranSet<ProductState<?>>, List<TranSet<ProductState<?>>>> grp : acc.acceptanceCondition) {

                int awaited = this.awaitedIndices[i];

                if (awaited == grp.right.size()) {
                    awaited = 0;
                }
                // TODO if we could rewrite it the routine in a way such that we
                // do not need grp.right.get(), then
                // we could use for the right side of GeneralisedRabinAcceptance
                // Collection instead of List'
                while (awaited < grp.right.size() && grp.right.get(awaited).contains(productState, valuation)) {
                    awaited++;
                }

                awaitedIndices[i] = grp.right.stream().filter(inf -> inf.contains(productState,valuation)).collect(Collectors.toList()).size();
                i++;
            }

            return new ProductDegenState(successor, awaitedIndices);
        }

        @Override
        public BitSet getSensitiveAlphabet() {
            return productState.getSensitiveAlphabet();
        }

        @Override
        public ValuationSetFactory getFactory() {
            return valuationSetFactory;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ProductDegenState state = (ProductDegenState) o;
            return Objects.equals(productState, state.productState) &&
                    Arrays.equals(awaitedIndices, state.awaitedIndices);
        }

        @Override
        public int hashCode() {
            return Objects.hash(productState, Arrays.hashCode(awaitedIndices));
        }
    }
}
