package rabinizer.automata;

import rabinizer.exec.Tuple;
import rabinizer.ltl.ValuationSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author jkretinsky
 */
public class DSGRA extends Automaton implements AccAutomatonInterface {

    DTGRARaw dtgra;
    AccTGR<? extends IState<?>> accTGR;
    AccSGR accSGR;

    public DSGRA(DTGRARaw dtgra) {
        super(dtgra.valuationSetFactory);
        this.dtgra = dtgra;
        trapState = new ProductAccState((Product.ProductState) dtgra.automaton.trapState, new HashMap<>());
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
    protected ProductAccState generateInitialState() {
        Map<Integer, Set<Integer>> accSets = new HashMap<>();
        for (int i = 0; i < accTGR.size(); i++) {
            accSets.put(i, new HashSet<>());
        }
        return new ProductAccState((Product.ProductState) dtgra.automaton.initialState, accSets);
    }

    @Override
    protected String accName() {
        String result = "acc-name: generalized-Rabin " + accSGR.size();
        for (GRabinPair<Set<ProductAccState>> anAccSGR : accSGR) {
            result += " " + (anAccSGR.getRight().size());
        }
        return result + "\n";
    }

    @Override
    protected String accTypeNumerical() {
        if (accSGR.isEmpty()) {
            return "0 f";
        }
        String result = "";
        int sum = 0;
        for (int i = 0; i < accSGR.size(); i++) {
            result += i == 0 ? "" : " | ";
            result += "Fin(" + sum + ")";
            sum++;
            for (Set<ProductAccState> set : accSGR.get(i).getRight()) {
                result += "&Inf(" + sum + ")";
                sum++;
            }
        }
        return sum + " " + result;
    }

    protected String stateAcc(ProductAccState s) {
        return "\n{" + accSGR.accSets(s) + "}";
    }

    public class ProductAccState extends Tuple<Product.ProductState, Map<Integer, Set<Integer>>> implements IState<ProductAccState> {

        public ProductAccState(Product.ProductState ps, Map<Integer, Set<Integer>> accSets) {
            super(ps, accSets);
        }

        @Override
        public String toString() {
            return getLeft() + " " + getRight();
        }

        @Override
        public ProductAccState getSuccessor(Set<String> valuation) {
            Map<Integer, Set<Integer>> accSets = new HashMap<>();
            for (int i = 0; i < accTGR.size(); i++) {
                accSets.put(i, new HashSet<>());
                GRabinPairT<? extends IState<?>> grp = accTGR.get(i);
                if (grp.getLeft() != null && grp.getLeft().get(getLeft()) != null
                        && grp.getLeft().get(getLeft()).contains(valuation)) {
                    accSets.get(i).add(-1);
                }
                for (int j = 0; j < grp.getRight().size(); j++) {
                    if (grp.getRight().get(j).get(getLeft()) != null
                            && grp.getRight().get(j).get(getLeft()).contains(valuation)) {
                        accSets.get(i).add(j);
                    }
                }
            }
            return new ProductAccState((Product.ProductState) dtgra.automaton.succ(getLeft(), valuation), accSets);
        }

        @Override
        public boolean isAccepting(Set<String> valuation) {
            return false;
        }

        @Override
        public Set<ValuationSet> partitionSuccessors() {
            return valuationSetFactory.createAllValuationSets(); // TODO symbolic
        }
    }
}
