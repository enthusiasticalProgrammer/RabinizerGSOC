package rabinizer.automata;

import rabinizer.ltl.GOperator;
import rabinizer.ltl.ValuationSet;
import rabinizer.ltl.ValuationSetFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author jkretinsky
 */
public class DTGRA extends Product implements AccAutomatonInterface {

    AccTGR acc;

    public DTGRA(FormulaAutomaton master, Map<GOperator, RabinSlave> slaves, ValuationSetFactory<String> factory) {
        super(master, slaves, factory);
    }

    public DTGRA(DTGRARaw raw) {
        super(raw.automaton);
        if (raw.accTGR != null) { // for computing the state space only (with no
            // acc. condition)
            this.acc = new AccTGR(raw.accTGR);
        }
    }

    @Override
    protected String accName() {
        String result = "acc-name: generalized-Rabin " + acc.size();
        for (GRabinPairT anAcc : acc) {
            result += " " + anAcc.getRight().size();
        }
        return result + "\n";
    }

    @Override
    protected String accTypeNumerical() {
        if (acc.isEmpty()) {
            return "0 f";
        }
        String result = "";
        int sum = 0;
        for (int i = 0; i < acc.size(); i++) {
            result += i == 0 ? "" : " | ";
            result += "Fin(" + sum + ")";
            sum++;
            List<TranSet<ProductState>> right = acc.get(i).getRight();
            for (int j = 0; j < right.size(); j++) {
                right.get(j);
                result += "&Inf(" + sum + ")";
                sum++;
            }
        }
        return sum + " " + result;
    }

    @Override
    protected String stateAcc(ProductState s) {
        return "";
    }

    @Override
    protected String outTransToHOA(ProductState s, Map<ProductState, Integer> statesToNumbers) {
        String result = "";
        Set<Set<ValuationSet>> productVs = new HashSet<>();
        productVs.add(transitions.row(s).keySet());
        System.out.println("transitions: " + transitions);
        System.out.println("s: " + s);
        System.out.println("trans.get(s):" + transitions.row(s));
        Set<ValuationSet> vSets;
        for (GRabinPairT rp : acc) {
            vSets = new HashSet<ValuationSet>();
            if (rp.getLeft().containsKey(s)) {
                vSets.add(rp.getLeft().get(s));
                vSets.add(rp.getLeft().get(s).complement());
            }
            productVs.add(vSets);
            for (TranSet<ProductState> ts : rp.getRight()) {
                vSets = new HashSet<ValuationSet>();
                if (ts.containsKey(s)) {
                    vSets.add(ts.get(s));
                    vSets.add(ts.get(s).complement());
                }
                productVs.add(vSets);
            }
        }
        vSets = new HashSet<>();
        productVs.remove(vSets);
        Set<ValuationSet> edges = generatePartitioning(productVs);
        for (ValuationSet vsSep : edges) {
            Set<String> v = vsSep.pickAny();
            result += "[" + vsSep.toFormula() + "] " + statesToNumbers.get(succ(s, v)) + " {" + acc.accSets(s, v)
                    + "}\n";
        }
        return result;
    }

    @Override
    public String acc() {
        return acc.toString();
    }

    @Override
    public int pairNumber() {
        return acc.size();
    }

}
