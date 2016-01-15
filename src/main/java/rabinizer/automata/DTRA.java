package rabinizer.automata;

import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import jhoafparser.consumer.HOAConsumerPrint;
import rabinizer.automata.output.HOAConsumerExtended;
import rabinizer.collections.Tuple;
import rabinizer.ltl.ValuationSet;
import rabinizer.ltl.ValuationSetFactory;

/**
 * TODO: decouple DTRA from DTGRARaw
 *
 * @author jkretinsky
 */
public class DTRA<T extends IState<T>> extends AccAutomaton<DTRA<T>.ProductDegenState> implements AccAutomatonInterface {

    DTGRARaw dtgra;
    AccTGR<DTRA<T>.ProductDegenState> accTGR;
    AccTR<DTRA<T>.ProductDegenState> accTR;

    public DTRA(DTGRARaw dtgra, ValuationSetFactory factory) {
        super(factory);
        this.dtgra = dtgra;
        trapState = new ProductDegenState((T) dtgra.automaton.trapState, new HashMap<>());
        accTGR = new AccTGR(dtgra.accTGR);
        generate();
        accTR = new AccTR(accTGR, this, valuationSetFactory);
    }

    @Override
    public String acc() {
        return accTR.toString();
    }

    @Override
    public int pairNumber() {
        return accTR.size();
    }

    @Override
    protected ProductDegenState generateInitialState() {
        Map<Integer, Integer> awaitedIndices = new HashMap<>();

        for (int i = 0; i < accTGR.size(); i++) {
            awaitedIndices.put(i, 0);
        }

        return new ProductDegenState((T) dtgra.automaton.initialState, awaitedIndices);
    }

    @Override
    public void toHOANew(HOAConsumer ho) throws HOAConsumerException {
        HOAConsumerExtended<DTRA<T>.ProductDegenState> hoa = new HOAConsumerExtended(ho, HOAConsumerExtended.AutomatonType.TRANSITION);
        hoa.setHeader(null, valuationSetFactory.getAlphabet());
        hoa.setInitialState(this.initialState);
        hoa.setAcceptanceCondition((List<GRabinPair<?>>) (List<?>) accTR);

        List<GRabinPairT<ProductDegenState>> acc = new ArrayList<>();

        accTR.stream().forEach(p -> acc.add(new GRabinPairT<ProductDegenState>(p.left, Collections.singletonList(p.right))));

        // split transitions according to accepting Sets:
        for (GRabinPairT<ProductDegenState> pair : acc) {
            Table<ProductDegenState, ValuationSet, ProductDegenState> toAdd = HashBasedTable.create();
            Table<ProductDegenState, ValuationSet, ProductDegenState> toRemove = HashBasedTable.create();
            if (pair.left != null) {
                for (Table.Cell<ProductDegenState, ValuationSet, ProductDegenState> currTrans : transitions
                        .cellSet()) {
                    if (pair.left.keySet().contains(currTrans.getRowKey())) {
                        ValuationSet valu = pair.left.get(currTrans.getRowKey()).clone();
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
            }
            if (pair.right != null) {
                for (TranSet<ProductDegenState> currAccSet : pair.right) {
                    for (Table.Cell<ProductDegenState, ValuationSet, ProductDegenState> currTrans : transitions
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
                }
                toRemove.cellSet().stream().forEach(cell -> transitions.remove(cell.getRowKey(), cell.getColumnKey()));
                transitions.putAll(toAdd);
                toRemove.clear();
                toAdd.clear();
            }
        }

        for (ProductDegenState s : states) {
            hoa.addState(s);
            for (Table.Cell<ProductDegenState, ValuationSet, ProductDegenState> trans : transitions.cellSet()) {
                if (trans.getRowKey().equals(s)) {
                    List<Integer> accSets = new ArrayList<>();
                    acc.stream()
                    .filter(pair -> pair.left != null && pair.left.get(s) != null
                    && pair.left.get(s).containsAll(trans.getColumnKey()))
                    .map(p -> hoa.getNumber(p.left)).forEach(x -> accSets.add(new Integer(x)));

                    List<GRabinPairT<?>> notAccepted = acc.stream()
                            .filter(pair -> pair.left == null || pair.left.get(s) == null
                            || !pair.left.get(s).containsAll(trans.getColumnKey()))
                            .collect(Collectors.toList());
                    for (GRabinPairT<ProductDegenState> pair : acc) {
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

    public class ProductDegenState extends Tuple<T, Map<Integer, Integer>> implements IState<ProductDegenState> {

        public ProductDegenState(T ps, Map<Integer, Integer> awaitedIndices) {
            super(ps, awaitedIndices);
        }

        @Override
        public String toString() {
            return left + " " + right;
        }

        @Override
        public ProductDegenState getSuccessor(Set<String> valuation) {
            Map<Integer, Integer> awaitedIndices = new HashMap<>();
            for (int i = 0; i < accTGR.size(); i++) {
                GRabinPairT<ProductDegenState> grp = accTGR.get(i);
                int awaited = right.get(i);
                // System.out.print("$$$"+v+awaited);
                if (awaited == grp.right.size()) {
                    awaited = 0;
                }
                while (awaited < grp.right.size() && grp.right.get(awaited).containsKey(left)
                        && grp.right.get(awaited).get(left).contains(valuation)) {
                    awaited++;
                } // System.out.println(awaited);
                awaitedIndices.put(i, awaited);
            }

            return new ProductDegenState((T) dtgra.automaton.getSuccessor((Product.ProductState) left, valuation), awaitedIndices);
        }

        @Override
        public Set<ValuationSet> partitionSuccessors() {
            return valuationSetFactory.createAllValuationSets(); // TODO symbolic
        }

        @Override
        public Set<String> getSensitiveAlphabet() {
            return valuationSetFactory.getAlphabet();
        }

        @Override
        public ValuationSetFactory getFactory() {
            return valuationSetFactory;
        }
    }
}
