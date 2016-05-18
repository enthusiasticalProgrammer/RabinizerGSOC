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

import jhoafparser.consumer.HOAConsumer;

import rabinizer.automata.output.HOAConsumerRabin;
import rabinizer.automata.output.HOAConsumerExtended;

import rabinizer.collections.valuationset.ValuationSet;
import rabinizer.collections.valuationset.ValuationSetFactory;

import javax.annotation.Nullable;
import java.util.*;

public class DTRA extends Automaton<DTRA.ProductDegenState> {

    final List<RabinPair<ProductDegenState>> accTR;
    private final DTGRA dtgra;

    public DTRA(DTGRA dtgra) {
        super(dtgra.valuationSetFactory);
        this.dtgra = dtgra;
        generate();

        accTR = new ArrayList<>();

        for (int i = 0; i < dtgra.acc.size(); i++) {
            GeneralizedRabinPair<Product.ProductState> grp = dtgra.acc.get(i);
            TranSet<ProductDegenState> fin = new TranSet<>(valuationSetFactory);
            TranSet<ProductDegenState> inf = new TranSet<>(valuationSetFactory);

            for (ProductDegenState s : getStates()) {
                ValuationSet vsFin = grp.fin.asMap().get(s.productState);
                if (vsFin != null) {
                    fin.addAll(s, vsFin);
                }

                if (s.awaitedIndices[i] == grp.infs.size()) {
                    inf.addAll(s, valuationSetFactory.createUniverseValuationSet());
                }
            }

            accTR.add(new RabinPair<>(fin, inf));
        }
    }



    @Override
    public HOAConsumerRabin getConsumer(HOAConsumer ho) {
        return new HOAConsumerRabin(ho, valuationSetFactory, getInitialState(), accTR);
    }

    @Override
    protected ProductDegenState generateInitialState() {
        return new ProductDegenState(dtgra.getInitialState(), new int[dtgra.acc.size()]);
    }

    public class ProductDegenState implements IState<ProductDegenState> {

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
        public ProductDegenState getSuccessor(BitSet valuation) {
            Product.ProductState successor = dtgra.getSuccessor(productState, valuation);

            if (successor == null) {
                return null;
            }

            int[] awaitedIndices = new int[dtgra.acc.size()];

            // TODO: Use listIterator
            for (int i = 0; i < dtgra.acc.size(); i++) {
                GeneralizedRabinPair<Product.ProductState> grp = dtgra.acc.get(i);

                int awaited = this.awaitedIndices[i];

                if (awaited == grp.infs.size()) {
                    awaited = 0;
                }

                while (awaited < grp.infs.size() && grp.infs.get(awaited).contains(productState, valuation)) {
                    awaited++;
                }

                awaitedIndices[i] = awaited;
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
            return Objects.hash(productState, awaitedIndices);
        }
    }
}
