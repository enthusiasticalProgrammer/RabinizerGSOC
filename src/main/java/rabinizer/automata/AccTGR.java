/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rabinizer.automata;

import rabinizer.bdd.Valuation;

import java.util.ArrayList;

public class AccTGR extends ArrayList<GRabinPairT> {

    /**
     *
     */
    private static final long serialVersionUID = 2472964991141498843L;

    public AccTGR(AccTGRRaw accTGR) {
        super();
        for (GRabinPairRaw grp : accTGR) {
            add(grp.order());
        }
    }

    String accSets(ProductState s, Valuation v) {
        String result = "";
        int sum = 0;
        for (GRabinPairT gRabinPairT : this) {
            if (gRabinPairT.left.containsKey(s) && gRabinPairT.left.get(s).contains(v)) {
                result += sum + " ";
            }
            sum++;
            for (TranSet<ProductState> ts : gRabinPairT.right) {
                if (ts.containsKey(s) && ts.get(s).contains(v)) {
                    result += sum + " ";
                }
                sum++;
            }
        }
        return result;
    }

    @Override
    public String toString() {
        String result = "Gen. Rabin acceptance condition";
        int i = 1;
        for (GRabinPairT pair : this) {
            result += "\nPair " + i + "\n" + pair;
            i++;
        }
        return result;
    }

}
