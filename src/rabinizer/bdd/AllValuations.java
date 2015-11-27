/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rabinizer.bdd;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author jkretinsky
 */
public class AllValuations {

    /**
     * All the possible valuations.
     * <p>
     * Created and populated by initializeValuations.
     */
    public static List<Valuation> allValuations;
    public static Set<ValuationSet> allValuationsAsSets;

    private static void enumerateValuations(boolean[] values, int id) {
        if (id <= 0) {
            values[0] = false;
            allValuations.add(new Valuation(values));
            values[0] = true;
            allValuations.add(new Valuation(values));
        } else {
            values[id] = false;
            enumerateValuations(values, id - 1);
            values[id] = true;
            enumerateValuations(values, id - 1);
        }
    }

    /**
     * Create the valuations corresponding to n variables and store them in
     * valuations. The memory consumption is in the order of 2^n.
     *
     * @param n
     * @return
     */
    public static void initializeValuations(int n) {
        allValuations = new ArrayList<Valuation>();
        boolean[] values = new boolean[n];
        enumerateValuations(values, n - 1);
        allValuationsAsSets = new HashSet<ValuationSet>();
        for (Valuation v : allValuations) {
            allValuationsAsSets.add(new ValuationSetExplicit(v));
        }
    }


    //public static BooleanConstant fTrue = new BooleanConstant(true);
    //public static BooleanConstant fFalse = new BooleanConstant(false);
}
