package rabinizer.automata;

import rabinizer.collections.Tuple;
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
    AccTGR<T> accTGR;
    AccTR<T> accTR;

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
            if (rp.left.containsKey(s)) {
                vSets.add(rp.left.get(s));
                vSets.add(rp.left.get(s).complement());
            }
            productVs.add(vSets);
            vSets = new HashSet<>();
            if (rp.right.containsKey(s)) {
                vSets.add(rp.right.get(s));
                vSets.add(rp.right.get(s).complement());
            }
            productVs.add(vSets);
        }
        vSets = new HashSet<>();
        productVs.remove(vSets);
        Set<ValuationSet> edges = generatePartitioning(productVs);
        for (ValuationSet vsSep : edges) {
            Set<String> v = vsSep.pickAny();
            result += "[" + vsSep.toFormula() + "] " + statesToNumbers.get(getSuccessor(s, v)) + " {" + accTR.accSets(s, v)
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
            return left + " " + right;
        }

        @Override
        public ProductDegenState getSuccessor(Set<String> valuation) {
            Map<Integer, Integer> awaitedIndices = new HashMap<>();
            for (int i = 0; i < accTGR.size(); i++) {
                GRabinPairT<T> grp = accTGR.get(i);
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
