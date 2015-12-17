package rabinizer.automata;

import rabinizer.exec.Tuple;
import rabinizer.ltl.ValuationSet;
import rabinizer.ltl.ValuationSetFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * TODO: decouple DTRA from DTGRARaw
 *
 * @author jkretinsky
 */
public class DTRA<T extends IState<T>> extends AccAutomaton<DTRA<T>.ProductDegenState> implements AccAutomatonInterface {

    DTGRARaw dtgra;
    AccTGR<? extends IState<?>> accTGR;
    AccTR<T> accTR;

    public DTRA(DTGRARaw dtgra, ValuationSetFactory<String> factory) {
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
    protected String accName() {
        return "acc-name: Rabin " + accTR.size() + "\n";
    }

    @Override
    protected String accTypeNumerical() {
        if (accTR.isEmpty()) {
            return "0 f";
        }
        String result = accTR.size() * 2 + " ";
        for (int i = 0; i < accTR.size(); i++) {
            result += i == 0 ? "" : " | ";
            result += "Fin(" + 2 * i + ")&Inf(" + (2 * i + 1) + ")";
        }
        return result;
    }

    @Override
    protected String stateAcc(ProductDegenState productDegenState) {
        return null;
    }

    @Override
    protected String outTransToHOA(ProductDegenState s, Map<ProductDegenState, Integer> statesToNumbers) {
        String result = "";
        Set<Set<ValuationSet>> productVs = new HashSet<>();
        productVs.add(transitions.row(s).keySet());
        Set<ValuationSet> vSets;
        for (RabinPair<? extends IState<?>> rp : accTR) {
            vSets = new HashSet<>();
            if (rp.getLeft().containsKey(s)) {
                vSets.add(rp.getLeft().get(s));
                vSets.add(rp.getLeft().get(s).complement());
            }
            productVs.add(vSets);
            vSets = new HashSet<>();
            if (rp.getRight().containsKey(s)) {
                vSets.add(rp.getRight().get(s));
                vSets.add(rp.getRight().get(s).complement());
            }
            productVs.add(vSets);
        }
        vSets = new HashSet<>();
        productVs.remove(vSets);
        Set<ValuationSet> edges = generatePartitioning(productVs);
        for (ValuationSet vsSep : edges) {
            Set<String> v = vsSep.pickAny();
            result += "[" + vsSep.toFormula() + "] " + statesToNumbers.get(succ(s, v)) + " {" + accTR.accSets(s, v)
                    + "}\n";
        }
        return result;
    }

    public class ProductDegenState extends Tuple<T, Map<Integer, Integer>> implements IState<ProductDegenState> {

        public ProductDegenState(T ps, Map<Integer, Integer> awaitedIndices) {
            super(ps, awaitedIndices);
        }

        @Override
        public String toString() {
            return getLeft() + " " + getRight();
        }

        @Override
        public ProductDegenState getSuccessor(Set<String> valuation) {
            Map<Integer, Integer> awaitedIndices = new HashMap<>();
            for (int i = 0; i < accTGR.size(); i++) {
                GRabinPairT<? extends IState<?>> grp = accTGR.get(i);
                int awaited = getRight().get(i);
                // System.out.print("$$$"+v+awaited);
                if (awaited == grp.getRight().size()) {
                    awaited = 0;
                }
                while (awaited < grp.getRight().size() && grp.getRight().get(awaited).containsKey(getLeft())
                        && grp.getRight().get(awaited).get(getLeft()).contains(valuation)) {
                    awaited++;
                } // System.out.println(awaited);
                awaitedIndices.put(i, awaited);
            }

            return new ProductDegenState((T) dtgra.automaton.succ((Product.ProductState) this.getLeft(), valuation), awaitedIndices);
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
