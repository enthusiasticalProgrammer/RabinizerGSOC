package rabinizer.automata;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import jhoafparser.consumer.HOAConsumerPrint;
import rabinizer.automata.output.HOAConsumerExtended;
import rabinizer.ltl.GOperator;
import rabinizer.ltl.ValuationSet;
import rabinizer.ltl.ValuationSetFactory;

/**
 * @author jkretinsky
 */
public class DTGRA extends Product implements AccAutomatonInterface {

    AccTGR<ProductState> acc;

    public DTGRA(Master master, Map<GOperator, RabinSlave> slaves, ValuationSetFactory factory,
            Collection<Optimisation> optimisations) {
        super(master, slaves, factory, optimisations);
    }

    public DTGRA(DTGRARaw raw) {
        super(raw.automaton.primaryAutomaton, raw.automaton.secondaryAutomata, raw.automaton.valuationSetFactory, Collections.emptySet());
        this.states.addAll(raw.automaton.states);
        this.initialState = raw.automaton.initialState;
        this.transitions.putAll(raw.automaton.transitions);
        this.edgeBetween.putAll(raw.automaton.edgeBetween);
        if (raw.accTGR != null) { // for computing the state space only (with no
            // acc. condition)
            this.acc = new AccTGR<ProductState>(raw.accTGR);
        }
    }

    @Override
    public String acc() {
        return acc.toString();
    }

    @Override
    public int pairNumber() {
        return acc.size();
    }

    @Override
    public void toHOANew(HOAConsumer ho) throws HOAConsumerException {
        HOAConsumerExtended hoa = new HOAConsumerExtended(ho, false);
        hoa.setHeader(new ArrayList<>(valuationSetFactory.getAlphabet()));
        hoa.setInitialState(this.initialState);
        hoa.setAcceptanceCondition((List<GRabinPair<?>>) (List<?>) acc);

        //split transitions according to accepting Sets:
        for(GRabinPairT<Product.ProductState> pair: acc){
            Table<Product.ProductState,ValuationSet,Product.ProductState> toAdd = HashBasedTable.create();
            Table<Product.ProductState,ValuationSet,Product.ProductState> toRemove = HashBasedTable.create();
            if(pair.left!=null){
                for(Table.Cell<Product.ProductState,ValuationSet,Product.ProductState> currTrans:transitions.cellSet()){
                    if(pair.left.keySet().contains(currTrans.getRowKey())){
                        ValuationSet valu=pair.left.get(currTrans.getRowKey()).clone();
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
                        if (currAccSet.keySet().contains(currTrans.getRowKey())) {
                            ValuationSet valu = currAccSet.get(currTrans.getRowKey()).clone();
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
            for (Table.Cell<Product.ProductState, ValuationSet, Product.ProductState> trans : transitions.cellSet()) {
                if (trans.getRowKey().equals(s)) {
                    List<Integer> accSets = new ArrayList<>();
                    acc.stream()
                    .filter(pair -> pair.left != null && pair.left.get(s) != null
                    && pair.left.get(s).containsAll(trans.getColumnKey()))
                    .map(p -> hoa.getNumber(p.left)).forEach(x -> accSets.add(new Integer(x)));

                    List<GRabinPairT<?>> notAccepted = acc.stream().filter(pair -> pair.left == null
                            || pair.left.get(s) == null
                            || !pair.left.get(s).containsAll(trans.getColumnKey()))
                            .collect(Collectors.toList());
                    for (GRabinPairT<ProductState> pair : acc) {
                        accSets.addAll(pair.right.stream()
                                .filter(inf -> inf != null && inf.get(s) != null
                                        && inf.get(s).containsAll(trans.getColumnKey()))
                                .map(inf -> hoa.getNumber(inf)).collect(Collectors.toList()));
                    }
                    hoa.addEdge(trans.getRowKey(), trans.getColumnKey().toFormula(), trans.getValue(), accSets);
                }
            }
        }
        hoa.done();

    }

}
