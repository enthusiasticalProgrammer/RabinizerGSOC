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
import omega_automaton.Edge;
import omega_automaton.acceptance.GeneralisedRabinAcceptance;
import omega_automaton.acceptance.RabinAcceptance;
import omega_automaton.collections.TranSet;
import omega_automaton.collections.Tuple;
import omega_automaton.collections.valuationset.ValuationSet;
import omega_automaton.collections.valuationset.ValuationSetFactory;
import rabinizer.automata.RabinSlave.State;

import javax.annotation.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class DTRA extends Automaton<DTRA.ProductDegenState, RabinAcceptance<DTRA.ProductDegenState>> {

    private static BitSet standardBitSet = new BitSet(0);

    private final ProductRabinizer dtgra;

    public DTRA(ProductRabinizer dtgra) {
        super(null, dtgra.getFactory());
        this.acceptance = new RabinAcceptance<DTRA.ProductDegenState>(new ArrayList<>());
        if (!(dtgra.getAcceptance() instanceof GeneralisedRabinAcceptance<?>)) {
            throw new IllegalArgumentException();
        }
        GeneralisedRabinAcceptance<ProductRabinizer.ProductState> acc = dtgra.getAcceptance();
        this.dtgra = dtgra;
        generate();

        int i = 0;
        for (Tuple<TranSet<ProductRabinizer.ProductState>, List<TranSet<ProductRabinizer.ProductState>>> grp : acc.unmodifiableCopyOfAcceptanceCondition()) {
            TranSet<ProductDegenState> fin = new TranSet<>(valuationSetFactory);
            TranSet<ProductDegenState> inf = new TranSet<>(valuationSetFactory);

            for (ProductDegenState s : getStates()) {
                ValuationSet vsFin = grp.left.asMap().get(s.productState);
                if (vsFin != null) {
                    fin.addAll(s, vsFin);
                }

                if (s.awaitedIndices[i] == grp.right.size()) {
                    inf.addAll(s, valuationSetFactory.createUniverseValuationSet());
                }
            }
            i++;
            acceptance.addRabinPair(fin, inf);
        }
    }

    @Override
    protected ProductDegenState generateInitialState() {
        return new ProductDegenState(dtgra.getInitialState(), new int[dtgra.getAcceptance().unmodifiableCopyOfAcceptanceCondition().size()]);
    }

    public class ProductDegenState implements AutomatonState<ProductDegenState> {

        public final ProductRabinizer.ProductState productState;
        public final int[] awaitedIndices;

        public ProductDegenState(ProductRabinizer.ProductState automatonState, int... awaitedIndices) {
            this.productState = automatonState;
            this.awaitedIndices = awaitedIndices;
        }

        @Override
        public String toString() {
            return productState + " " + Arrays.toString(awaitedIndices);
        }

        @Nullable
        @Override
        public Edge<ProductDegenState> getSuccessor(BitSet valuation) {
            Edge<ProductRabinizer.ProductState> successor = dtgra.getSuccessor(productState, valuation);

            if (successor == null) {
                return null;
            }

            GeneralisedRabinAcceptance<ProductRabinizer.ProductState> acc = dtgra.getAcceptance();
            List<Tuple<TranSet<Product<State>.ProductState>, List<TranSet<Product<State>.ProductState>>>> accList = acc.unmodifiableCopyOfAcceptanceCondition();

            int[] awaitedIndices = new int[accList.size()];

            // TODO: Use listIterator

            for (int i = 0; i < accList.size(); i++) {
                Tuple<TranSet<Product<State>.ProductState>, List<TranSet<Product<State>.ProductState>>> grp = accList.get(i);

                int awaited = this.awaitedIndices[i];

                if (awaited == grp.right.size()) {
                    awaited = 0;
                }
                // TODO if we could rewrite it the routine in a way such that we
                // do not need the position at the list, then
                // we could use for the right side of GeneralisedRabinAcceptance
                // Collection instead of List'
                Iterator<TranSet<Product<State>.ProductState>> infIterator = grp.right.iterator();
                while (infIterator.hasNext()) {
                    if (!infIterator.next().contains(productState, valuation)) {
                        break;
                    }
                    awaited++;
                }

                awaitedIndices[i] = grp.right.stream().filter(inf -> inf.contains(productState, valuation)).collect(Collectors.toList()).size();
                i++;
            }

            return new Edge<>(new ProductDegenState(successor.successor, awaitedIndices), standardBitSet);
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
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            ProductDegenState state = (ProductDegenState) o;
            return Objects.equals(productState, state.productState) && Arrays.equals(awaitedIndices, state.awaitedIndices);
        }

        @Override
        public int hashCode() {
            return Objects.hash(productState, Arrays.hashCode(awaitedIndices));
        }
    }
}
