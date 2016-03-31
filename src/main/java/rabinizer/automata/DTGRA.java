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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DTGRA extends Automaton<Product.ProductState> {

    final @NotNull List<GeneralizedRabinPair<Product.ProductState>> acc;

    public DTGRA(@NotNull Product product, @Nullable List<GeneralizedRabinPair<Product.ProductState>> acc) {
        super(product);

        if (acc != null) {
            this.acc = acc;
        } else {
            this.acc = Collections.emptyList();
        }
    }

    @Override
    public void toHOA(HOAConsumer ho) throws HOAConsumerException {
        if (states.isEmpty()) {
            ho.notifyHeaderStart("v1");
            ho.setNumberOfStates(0);
            ho.setAPs(Collections.emptyList());
            ho.setAcceptanceCondition(0, new BooleanExpression<>(false));
            ho.notifyBodyStart();
            ho.notifyEnd();
            return;
        }

        HOAConsumerExtended<Product.ProductState> hoa = new HOAConsumerExtended<>(ho, HOAConsumerExtended.AutomatonType.TRANSITION);
        hoa.setHeader(null, valuationSetFactory.getAlphabet());
        hoa.setInitialState(this.initialState);
        hoa.setAcceptanceCondition(acc);

        //split transitions according to accepting Sets:
        for (GeneralizedRabinPair<Product.ProductState> pair : acc) {
            Table<Product.ProductState, ValuationSet, Product.ProductState> toAdd = HashBasedTable.create();
            Table<Product.ProductState, ValuationSet, Product.ProductState> toRemove = HashBasedTable.create();

            for (Table.Cell<Product.ProductState, ValuationSet, Product.ProductState> currTrans : transitions.cellSet()) {
                if (pair.fin.asMap().containsKey(currTrans.getRowKey())) {
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

            for (TranSet<Product.ProductState> currAccSet : pair.infs) {
                transitions.cellSet().stream().filter(currTrans -> currAccSet.asMap().containsKey(currTrans.getRowKey())).forEach(currTrans -> {
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
                });

                toRemove.cellSet().stream()
                        .forEach(cell -> transitions.remove(cell.getRowKey(), cell.getColumnKey()));
                transitions.putAll(toAdd);
                toRemove.clear();
                toAdd.clear();
            }
        }

        for (Product.ProductState s : states) {
            hoa.addState(s);

            for (Map.Entry<ValuationSet, Product.ProductState> trans : transitions.row(s).entrySet()) {
                List<Integer> accSets = acc.stream()
                        .filter(pair -> pair.fin.containsAll(s, trans.getKey()))
                        .map(p -> hoa.getNumber(p.fin)).collect(Collectors.toList());

                for (GeneralizedRabinPair<Product.ProductState> pair : acc) {
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
}
