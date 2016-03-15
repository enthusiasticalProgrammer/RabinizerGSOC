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

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rabinizer.automata.output.HOAConsumerExtended;
import rabinizer.collections.valuationset.ValuationSet;
import rabinizer.collections.valuationset.ValuationSetFactory;

import java.util.*;
import java.util.stream.Collectors;

public class DTRA extends Automaton<DTRA.ProductDegenState> {

    private final DTGRA dtgra;
    final List<RabinPair<ProductDegenState>> accTR;

    public DTRA(DTGRA dtgra) {
        super(dtgra.valuationSetFactory);
        this.dtgra = dtgra;
        trapState = new ProductDegenState(dtgra.trapState, new int[dtgra.acc.size()]);
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
    public void toHOA(HOAConsumer ho) throws HOAConsumerException {
        HOAConsumerExtended<ProductDegenState> hoa = new HOAConsumerExtended<>(ho, HOAConsumerExtended.AutomatonType.TRANSITION);
        hoa.setHeader(null, valuationSetFactory.getAlphabet());
        hoa.setInitialState(this.initialState);
        hoa.setAcceptanceCondition2(accTR);

        List<GeneralizedRabinPair<ProductDegenState>> acc = new ArrayList<>();
        accTR.stream().forEach(p -> acc.add(new GeneralizedRabinPair<>(p)));

        // split transitions according to accepting Sets:
        for (GeneralizedRabinPair<ProductDegenState> pair : acc) {
            Table<ProductDegenState, ValuationSet, ProductDegenState> toAdd = HashBasedTable.create();
            Table<ProductDegenState, ValuationSet, ProductDegenState> toRemove = HashBasedTable.create();
            for (Table.Cell<ProductDegenState, ValuationSet, ProductDegenState> currTrans : transitions.cellSet()) {
                if (pair.fin.asMap().keySet().contains(currTrans.getRowKey())) {
                    ValuationSet valu = pair.fin.asMap().get(currTrans.getRowKey()).clone();
                    valu.retainAll(currTrans.getColumnKey());
                    if (!valu.isEmpty() && !valu.equals(currTrans.getColumnKey())) {
                        toRemove.put(currTrans.getRowKey(), currTrans.getColumnKey(), currTrans.getValue());
                        toAdd.put(currTrans.getRowKey(), valu, currTrans.getValue());
                        ValuationSet valu2 = this.valuationSetFactory.createUniverseValuationSet();
                        valu2.retainAll(currTrans.getColumnKey());
                        valu2.retainAll(valu.complement());
                        toAdd.put(currTrans.getRowKey(), valu2, currTrans.getValue());
                    }
                }
            }

            toRemove.cellSet().stream().forEach(cell -> transitions.remove(cell.getRowKey(), cell.getColumnKey()));
            transitions.putAll(toAdd);
            toRemove.clear();
            toAdd.clear();
            for (TranSet<ProductDegenState> currAccSet : pair.infs) {
                for (Table.Cell<ProductDegenState, ValuationSet, ProductDegenState> currTrans : transitions
                        .cellSet()) {
                    if (currAccSet.asMap().keySet().contains(currTrans.getRowKey())) {
                        ValuationSet valu = currAccSet.asMap().get(currTrans.getRowKey()).clone();
                        valu.retainAll(currTrans.getColumnKey());
                        if (!valu.isEmpty() && !valu.equals(currTrans.getColumnKey())) {
                            toRemove.put(currTrans.getRowKey(), currTrans.getColumnKey(), currTrans.getValue());
                            toAdd.put(currTrans.getRowKey(), valu, currTrans.getValue());
                            ValuationSet valu2 = this.valuationSetFactory.createUniverseValuationSet();
                            valu2.retainAll(currTrans.getColumnKey());
                            valu2.retainAll(valu.complement());
                            toAdd.put(currTrans.getRowKey(), valu2, currTrans.getValue());
                        }
                    }
                }
            }
            toRemove.cellSet().stream().forEach(cell -> transitions.remove(cell.getRowKey(), cell.getColumnKey()));
            transitions.putAll(toAdd);
            toRemove.clear();
            toAdd.clear();
        }

        for (ProductDegenState s : states) {
            hoa.addState(s);

            for (Map.Entry<ValuationSet, ProductDegenState> trans : transitions.row(s).entrySet()) {
                List<Integer> accSets = acc.stream()
                        .filter(pair -> pair.fin.containsAll(s, trans.getKey()))
                        .map(p -> hoa.getNumber(p.fin))
                        .collect(Collectors.toList());

                for (GeneralizedRabinPair<ProductDegenState> pair : acc) {
                    pair.infs.stream()
                            .filter(inf -> inf != null && inf.containsAll(s, trans.getKey()))
                            .map(hoa::getNumber)
                            .forEach(accSets::add);
                }

                hoa.addEdge(s, trans.getKey().toFormula(), trans.getValue(), accSets);
            }

            hoa.stateDone();
        }

        hoa.done();
    }

    @Override
    protected @NotNull ProductDegenState generateInitialState() {
        return new ProductDegenState(dtgra.getInitialState(), new int[dtgra.acc.size()]);
    }

    public class ProductDegenState implements IState<ProductDegenState> {

        public final @NotNull Product.ProductState productState;
        public final int[] awaitedIndices;

        public ProductDegenState(@NotNull Product.ProductState ps, int... awaitedIndices) {
            this.productState = ps;
            this.awaitedIndices = awaitedIndices;
        }

        @Override
        public String toString() {
            return productState + " " + awaitedIndices;
        }

        @Override
        public @Nullable ProductDegenState getSuccessor(@NotNull Set<String> valuation) {
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
        public @NotNull Set<String> getSensitiveAlphabet() {
            return productState.getSensitiveAlphabet();
        }

        @Override
        public @NotNull ValuationSetFactory getFactory() {
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
