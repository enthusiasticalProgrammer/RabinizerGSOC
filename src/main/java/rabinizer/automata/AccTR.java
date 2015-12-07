/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rabinizer.automata;

import rabinizer.ltl.ValuationSet;
import rabinizer.ltl.ValuationSetFactory;

import java.util.ArrayList;
import java.util.Set;


/**
 * @author jkretinsky
 */
public class AccTR extends ArrayList<RabinPair<ProductDegenState>> {

    /**
     *
     */
    private static final long serialVersionUID = -5442515295977731129L;


    public AccTR(AccTGR accTGR, DTRA dtra, ValuationSetFactory<String> valuationSetFactory) {
        super();
        for (int i = 0; i < accTGR.size(); i++) {
            GRabinPairT grp = accTGR.get(i);
            TranSet<ProductDegenState> fin = new TranSet<>(valuationSetFactory);
            TranSet<ProductDegenState> inf = new TranSet<>(valuationSetFactory);
            for (ProductDegenState s : dtra.states) {
                ValuationSet vsFin = grp.left.get(s.left);
                if (vsFin != null) {
                    fin.add(s, vsFin);
                }
                if (s.right.get(i) == grp.right.size()) {
                    inf.add(s, valuationSetFactory.createUniverseValuationSet());
                }
            }
            this.add(new RabinPair<>(fin, inf));
        }
    }

    @Override
    public String toString() {
        String result = "Rabin transition-based acceptance condition";
        int i = 1;
        for (RabinPair pair : this) {
            result += "\nPair " + i + "\n" + pair;
            i++;
        }
        return result;
    }

    String accSets(ProductDegenState s, Set<String> v) {
        String result = "";
        for (int i = 0; i < size(); i++) {
            if (get(i).left.containsKey(s) && get(i).left.get(s).contains(v)) {
                result += 2 * i + " ";
            }
            if (get(i).right.containsKey(s) && get(i).right.get(s).contains(v)) {
                result += (2 * i + 1) + " ";
            }
        }
        return result;
    }

}
