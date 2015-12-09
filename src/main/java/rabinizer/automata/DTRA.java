/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rabinizer.automata;

import rabinizer.ltl.ValuationSet;
import rabinizer.ltl.ValuationSetFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author jkretinsky
 */
public class DTRA extends AccAutomaton<ProductDegenState> implements AccAutomatonInterface {

    DTGRARaw dtgra;
    AccTGR accTGR;
    AccTR accTR;

    public DTRA(DTGRARaw dtgra, ValuationSetFactory<String> factory) {
        super(factory);
        this.dtgra = dtgra;
        accTGR = new AccTGR(dtgra.accTGR);
        generate();
        accTR = new AccTR(accTGR, this, valuationSetFactory);
    }

    @Override
    protected ProductDegenState generateInitialState() {
        Map<Integer, Integer> awaitedIndices = new HashMap<>();
        for (int i = 0; i < accTGR.size(); i++) {
            awaitedIndices.put(i, 0);
        }
        return new ProductDegenState(dtgra.automaton.initialState, awaitedIndices);
    }

    @Override
    protected ProductDegenState generateSuccState(ProductDegenState s, ValuationSet vs) {
        Set<String> v = vs.pickAny();
        Map<Integer, Integer> awaitedIndices = new HashMap<>();
        for (int i = 0; i < accTGR.size(); i++) {
            GRabinPairT grp = accTGR.get(i);
            int awaited = s.right.get(i);
            // System.out.print("$$$"+v+awaited);
            if (awaited == grp.right.size()) {
                awaited = 0;
            }
            while (awaited < grp.right.size() && grp.right.get(awaited).containsKey(s.left)
                    && grp.right.get(awaited).get(s.left).contains(v)) {
                awaited++;
            } // System.out.println(awaited);
            awaitedIndices.put(i, awaited);
        }
        return new ProductDegenState(dtgra.automaton.succ(s.left, v), awaitedIndices);
    }

    @Override
    protected Set<ValuationSet> generateSuccTransitions(ProductDegenState s) {
        return valuationSetFactory.createAllValuationSets(); // TODO symbolic
    }

    @Override
    public String acc() {
        return accTR.toString();
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
    protected String stateAcc(ProductDegenState s) {
        return "";
    }

    @Override
    protected String outTransToHOA(ProductDegenState s, Map<ProductDegenState, Integer> statesToNumbers) {
        String result = "";
        Set<Set<ValuationSet>> productVs = new HashSet<>();
        productVs.add(transitions.row(s).keySet());
        Set<ValuationSet> vSets;
        for (RabinPair<ProductDegenState> rp : accTR) {
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
            result += "[" + vsSep.toFormula() + "] "
                    + statesToNumbers.get(succ(s, v)) + " {" + accTR.accSets(s, v) + "}\n";
        }
        return result;
    }

    public int pairNumber() {
        return accTR.size();
    }

}
