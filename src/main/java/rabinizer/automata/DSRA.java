package rabinizer.automata;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import rabinizer.collections.Tuple;
import rabinizer.ltl.ValuationSet;
import rabinizer.ltl.ValuationSetFactory;

/**
 * @author jkretinsky
 */
public class DSRA extends Automaton<DSRA.ProductDegenAccState> implements AccAutomatonInterface {

    public AccSR accSR;
    DTRA dtra;
    AccTR<? extends IState<?>> accTR;
    Map<IState, Set<Integer>> stateAcceptance;

    public DSRA(DTRA<Product.ProductState> dtra, ValuationSetFactory factory) {
        super(factory);
        this.dtra = dtra;
        trapState = new ProductDegenAccState(dtra.trapState, new HashSet<>());
        this.accTR = dtra.accTR;
        stateAcceptance = new HashMap<>();
        for (DTRA.ProductDegenState s : dtra.getStates()) {
            stateAcceptance.put(s, new HashSet<>());
            for (int i = 0; i < accTR.size(); i++) {
                RabinPair<? extends IState<?>> rp = accTR.get(i);
                if (valuationSetFactory.createAllValuationSets().equals(rp.left.get(s))) {
                    stateAcceptance.get(s).add(2 * i);
                } else if (valuationSetFactory.createAllValuationSets().equals(rp.right.get(s))) {
                    stateAcceptance.get(s).add(2 * i + 1);
                }
            }
        }
        generate();
        accSR = new AccSR(accTR, this);
    }

    @Override
    public String acc() {
        return accSR.toString();
    }

    @Override
    public int pairNumber() {
        return accSR.size() / 2;
    }

    @Override
    protected ProductDegenAccState generateInitialState() {
        return new ProductDegenAccState(dtra.initialState, stateAcceptance.get(dtra.initialState));
    }

    public class ProductDegenAccState extends Tuple<IState, Set<Integer>> implements IState<ProductDegenAccState> {

        public ProductDegenAccState(IState pds, Set<Integer> accSets) {
            super(pds, accSets);
        }

        @Override
        public String toString() {
            String result = left.toString();
            int[] orderedSets = new int[right.size()];
            int i = 0;
            for (Integer set : right) {
                orderedSets[i] = set;
                i++;
            }
            Arrays.sort(orderedSets);
            for (i = 0; i < orderedSets.length; i++) {
                int j = orderedSets[i];
                result += " " + (j % 2 == 1 ? "+" : "-") + (j / 2 + 1);
            }
            return result;
        }

        @Override
        public ProductDegenAccState getSuccessor(Set<String> valuation) {
            IState succ = dtra.getSuccessor(left, valuation);
            Set<Integer> accSets = new HashSet<>(stateAcceptance.get(succ));
            for (int i = 0; i < accTR.size(); i++) {
                RabinPair<? extends IState<?>> rp = accTR.get(i);
                if (rp.left != null && rp.left.get(left) != null
                        && rp.left.get(left).contains(valuation) && !stateAcceptance.get(left).contains(2 * i)) {
                    // acceptance
                    // dealt
                    // with
                    // already
                    // in s
                    accSets.add(2 * i);
                }
                if (rp.right != null && rp.right.get(left) != null
                        && rp.right.get(left).contains(valuation)
                        && !stateAcceptance.get(left).contains(2 * i + 1)) {
                    accSets.add(2 * i + 1);
                }
                if (accSets.contains(2 * i) && accSets.contains(2 * i + 1)) {
                    accSets.remove(2 * i + 1);
                }
            }
            return new ProductDegenAccState(succ, accSets);
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
    public void toHOANew(HOAConsumer hoa) throws HOAConsumerException {
        throw new UnsupportedOperationException();
    }
}
