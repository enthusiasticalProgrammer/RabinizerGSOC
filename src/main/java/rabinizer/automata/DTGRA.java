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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import rabinizer.automata.output.HOAConsumerExtended;
import rabinizer.collections.valuationset.ValuationSet;

public class DTGRA extends Product implements AccAutomatonInterface {

    private AccTGR acc;

    public DTGRA(DTGRARaw raw) {
        super(raw.automaton.primaryAutomaton, raw.automaton.secondaryAutomata, raw.automaton.valuationSetFactory, Collections.emptySet());
        this.states.addAll(raw.automaton.states);
        this.initialState = raw.automaton.initialState;
        this.transitions.putAll(raw.automaton.transitions);
        this.edgeBetween.putAll(raw.automaton.edgeBetween);
        if (raw.accTGR != null) { // for computing the state space only (with no
            // acc. condition)
            this.acc = new AccTGR(raw.accTGR);
        }
    }

    @Override
    public void acc(PrintStream p) {
        p.print(acc);
    }

    @Override
    public int pairNumber() {
        return acc.size();
    }

    @Override
    public void toHOA(HOAConsumer ho) throws HOAConsumerException {
        HOAConsumerExtended<Product.ProductState> hoa = new HOAConsumerExtended<>(ho, HOAConsumerExtended.AutomatonType.TRANSITION);
        hoa.setHeader(null, valuationSetFactory.getAlphabet());
        hoa.setInitialState(this.initialState);
        hoa.setAcceptanceCondition(acc);

        //split transitions according to accepting Sets:
        for(GRabinPair<TranSet<ProductState>> pair: acc){
            Table<Product.ProductState,ValuationSet,Product.ProductState> toAdd = HashBasedTable.create();
            Table<Product.ProductState,ValuationSet,Product.ProductState> toRemove = HashBasedTable.create();

            if (pair.left != null){
                for(Table.Cell<Product.ProductState,ValuationSet,Product.ProductState> currTrans : transitions.cellSet()){
                    if (pair.left.asMap().containsKey(currTrans.getRowKey())){
                        ValuationSet valu = pair.left.asMap().get(currTrans.getRowKey()).clone();
                        valu.retainAll(currTrans.getColumnKey());
                        if(!valu.isEmpty() && !valu.equals(currTrans.getColumnKey())){
                            toRemove.put(currTrans.getRowKey(),currTrans.getColumnKey(),currTrans.getValue());
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
            }

            if (pair.right != null) {
                for (TranSet<Product.ProductState> currAccSet : pair.right) {
                    for (Table.Cell<Product.ProductState, ValuationSet, Product.ProductState> currTrans : transitions
                            .cellSet()) {
                        if (currAccSet.asMap().containsKey(currTrans.getRowKey())) {
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
                    toRemove.cellSet().stream()
                            .forEach(cell -> transitions.remove(cell.getRowKey(), cell.getColumnKey()));
                    transitions.putAll(toAdd);
                    toRemove.clear();
                    toAdd.clear();
                }
            }
        }

        for (ProductState s : states) {
            hoa.addState(s);

            for (Map.Entry<ValuationSet, ProductState> trans : transitions.row(s).entrySet()) {
                List<Integer> accSets = acc.stream()
                    .filter(pair -> pair.left != null && pair.left.containsAll(s, trans.getKey()))
                    .map(p -> hoa.getNumber(p.left)).collect(Collectors.toList());

                for (GRabinPair<TranSet<ProductState>> pair : acc) {
                    pair.right.stream()
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
