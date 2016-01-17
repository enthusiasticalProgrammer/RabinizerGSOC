package rabinizer.automata;

import java.io.OutputStream;
import java.util.ArrayList;

import org.jetbrains.annotations.NotNull;
import rabinizer.ltl.ValuationSetFactory;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Table;

import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import jhoafparser.consumer.HOAConsumerPrint;
import rabinizer.automata.DSGRA.ProductAccState;
import rabinizer.automata.output.HOAConsumerExtended;
import rabinizer.collections.Tuple;
import rabinizer.ltl.ValuationSet;

/**
 * @author jkretinsky
 */
public class DSGRA extends Automaton<DSGRA.ProductAccState> implements AccAutomatonInterface {

    DTGRARaw dtgra;
    AccTGR<ProductAccState> accTGR;
    AccSGR accSGR;

    public DSGRA(DTGRARaw dtgra) {
        super(dtgra.valuationSetFactory, true);
        this.dtgra = dtgra;
        trapState = new ProductAccState(dtgra.automaton.trapState, new HashMap<>());
        accTGR = new AccTGR(dtgra.accTGR);
        generate();
        accSGR = new AccSGR(accTGR, this);
    }

    @Override
    public String acc() {
        return accSGR.toString();
    }

    @Override
    public int pairNumber() {
        return accSGR.size();
    }

    @Override
    protected @NotNull ProductAccState generateInitialState() {
        Map<Integer, Set<Integer>> accSets = new HashMap<>();
        for (int i = 0; i < accTGR.size(); i++) {
            accSets.put(i, new HashSet<>());
        }
        return new ProductAccState(dtgra.automaton.initialState, accSets);
    }

    public class ProductAccState extends Tuple<Product.ProductState, Map<Integer, Set<Integer>>> implements IState<ProductAccState> {

        public ProductAccState(Product.ProductState ps, Map<Integer, Set<Integer>> accSets) {
            super(ps, accSets);
        }

        @Override
        public String toString() {
            return left + " " + right;
        }

        @Override
        public ProductAccState getSuccessor(@NotNull Set<String> valuation) {
            Map<Integer, Set<Integer>> accSets = new HashMap<>();
            for (int i = 0; i < accTGR.size(); i++) {
                accSets.put(i, new HashSet<>());
                GRabinPairT<ProductAccState> grp = accTGR.get(i);
                if (grp.left != null && grp.left.get(left) != null
                        && grp.left.get(left).contains(valuation)) {
                    accSets.get(i).add(-1);
                }
                for (int j = 0; j < grp.right.size(); j++) {
                    if (grp.right.get(j).get(left) != null
                            && grp.right.get(j).get(left).contains(valuation)) {
                        accSets.get(i).add(j);
                    }
                }
            }
            return new ProductAccState(dtgra.automaton.getSuccessor(left, valuation), accSets);
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

    @Override
    public void toHOANew(HOAConsumer ho) throws HOAConsumerException {
        HOAConsumerExtended hoa = new HOAConsumerExtended(ho, HOAConsumerExtended.AutomatonType.STATE);
        hoa.setHeader(null, valuationSetFactory.getAlphabet());
        hoa.setInitialState(this.initialState);
        hoa.setAcceptanceCondition(accSGR);

        for (ProductAccState s : states) {
            List<Integer> accSets = new ArrayList<>(accSGR.stream().filter(pair -> pair.left != null && pair.left.contains(s))
                    .map(p -> hoa.getNumber(p.left)).collect(Collectors.toList()));
            for (GRabinPair<Set<ProductAccState>> pair : accSGR) {
                accSets.addAll(pair.right.stream().filter(inf -> inf.contains(s))
                        .map(hoa::getNumber).collect(Collectors.toList()));
            }
            hoa.addState(s, accSets);
        }

        for (Table.Cell<ProductAccState, ValuationSet, ProductAccState> trans : transitions.cellSet()) {
            hoa.addEdge(trans.getRowKey(), trans.getColumnKey().toFormula(), trans.getValue());
        }
        hoa.done();

    }
}
