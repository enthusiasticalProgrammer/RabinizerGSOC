package rabinizer.automata;

import rabinizer.ltl.GOperator;
import rabinizer.ltl.Simplifier;
import rabinizer.ltl.ValuationSet;
import rabinizer.ltl.ValuationSetFactory;

import java.util.*;

/**
 * @author jkretinsky
 */
public class DTGRA extends Product implements AccAutomatonInterface {

    AccTGR<? extends IState<?>> acc;

    public DTGRA(Master master, Map<GOperator, RabinSlave> slaves, ValuationSetFactory factory, Collection<Optimisation> optimisations) {
        super(master, slaves, factory, optimisations);
    }

    public DTGRA(DTGRARaw raw) {
        super(raw.automaton.primaryAutomaton, raw.automaton.secondaryAutomata, raw.automaton.valuationSetFactory, Collections.emptySet());
        if (raw.accTGR != null) { // for computing the state space only (with no
            // acc. condition)
            this.acc = new AccTGR(raw.accTGR);
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
    protected String accName() {
        String result = "acc-name: generalized-Rabin " + acc.size();
        for (GRabinPairT<?> anAcc : acc) {
            result += " " + anAcc.right.size();
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
            List<? extends TranSet<? extends IState<?>>> right = acc.get(i).right;
            for (int j = 0; j < right.size(); j++) {
                right.get(j);
                result += "&Inf(" + sum + ")";
                sum++;
            }
        }
        return sum + " " + result;
    }

    protected String stateAcc(AbstractProductState s) {
        return "";
    }

    protected String outTransToHOA(ProductState s, Map<IState, Integer> statesToNumbers) {
        String result = "";
        Set<Set<ValuationSet>> productVs = new HashSet<>();
        productVs.add(transitions.row(s).keySet());
        System.out.println("transitions: " + transitions);
        System.out.println("s: " + s);
        System.out.println("trans.get(s):" + transitions.row(s));
        Set<ValuationSet> vSets;
        for (GRabinPairT<? extends IState<?>> rp : acc) {
            vSets = new HashSet<>();
            if (rp.left.containsKey(s)) {
                vSets.add(rp.left.get(s));
                vSets.add(rp.left.get(s).complement());
            }
            productVs.add(vSets);
            for (TranSet<? extends IState<?>> ts : rp.right) {
                vSets = new HashSet<>();
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
            result += "[" + Simplifier.simplify(vsSep.toFormula(), Simplifier.Strategy.PROPOSITIONAL) + "] " + getId(statesToNumbers, getSuccessor(s, v)) + " {" + acc.accSets(s, v)
                    + "}\n";
        }
        return result;
    }

}
