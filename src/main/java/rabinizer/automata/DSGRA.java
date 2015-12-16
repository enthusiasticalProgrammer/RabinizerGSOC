package rabinizer.automata;

import rabinizer.ltl.ValuationSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author jkretinsky
 */
public class DSGRA extends Automaton<ProductAccState> implements AccAutomatonInterface {

    DTGRARaw dtgra;
    AccTGR accTGR;
    AccSGR accSGR;

    public DSGRA(DTGRARaw dtgra) {
        super(dtgra.valuationSetFactory);
        this.dtgra = dtgra;
        trapState = new ProductAccState(dtgra.automaton.trapState, new HashMap<>());
        accTGR = new AccTGR(dtgra.accTGR);
        generate();
        accSGR = new AccSGR(accTGR, this);
    }

    @Override
    protected ProductAccState generateInitialState() {
        Map<Integer, Set<Integer>> accSets = new HashMap<>();
        for (int i = 0; i < accTGR.size(); i++) {
            accSets.put(i, new HashSet<>());
        }
        return new ProductAccState(dtgra.automaton.initialState, accSets);
    }

    @Override
    protected ProductAccState generateSuccState(ProductAccState s, ValuationSet vs) {
        Set<String> v = vs.pickAny();
        Map<Integer, Set<Integer>> accSets = new HashMap<>();
        for (int i = 0; i < accTGR.size(); i++) {
            accSets.put(i, new HashSet<>());
            GRabinPairT grp = accTGR.get(i);
            if (grp.getLeft() != null && grp.getLeft().get(s.getLeft()) != null
                    && grp.getLeft().get(s.getLeft()).contains(v)) {
                accSets.get(i).add(-1);
            }
            for (int j = 0; j < grp.getRight().size(); j++) {
                if (grp.getRight().get(j).get(s.getLeft()) != null
                        && grp.getRight().get(j).get(s.getLeft()).contains(v)) {
                    accSets.get(i).add(j);
                }
            }
        }
        return new ProductAccState(dtgra.automaton.succ(s.getLeft(), v), accSets);
    }

    @Override
    protected Set<ValuationSet> generateSuccTransitions(ProductAccState s) {
        return valuationSetFactory.createAllValuationSets(); // TODO symbolic
    }

    @Override
    public String acc() {
        return accSGR.toString();
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

    @Override
    protected String stateAcc(ProductAccState s) {
        return "\n{" + accSGR.accSets(s) + "}";
    }

    @Override
    public int pairNumber() {
        return accSGR.size();
    }

}
