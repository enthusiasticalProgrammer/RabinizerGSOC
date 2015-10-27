/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rabinizer.bdd;

import java.util.*;
import rabinizer.formulas.*;
import net.sf.javabdd.*;

/**
 * A valuation is a map from atomic propositions to boolean values. In the AST
 * atomic propositions are integers.
 *
 * @author Jan Kretinsky & Ruslan Ledesma-Garza
 *
 */
public class Valuation extends HashMap<Integer, Boolean> {

    /**
     * An automatically generated serial version.
     */
    private static final long serialVersionUID = -5927454815102468383L;

    //private static BDDFactory bf;
    public Valuation() {
        super();
    }
    
    public Valuation(int n) {
        super();
        for (int i = 0; i < n; i++) {
            this.put(i, false);
        }
    }

    public Valuation(boolean[] values) {
        super();
        for (int i = 0; i < values.length; i++) {
            this.put(i, values[i]);
        }
    }

    public Valuation(List<Boolean> values) {
        super();
        for (int i = 0; i < values.size(); i++) {
            this.put(i, values.get(i));
        }
    }
    
    public Valuation set(int var, boolean value) {
        this.put(var, value);
        return this;
    }

    private String strValuation = null;

    public String toString() {
        if (strValuation == null) {
            strValuation = "{";
            boolean first = true;
            for (Map.Entry<Integer, Boolean> e : this.entrySet()) {
                if (first && e.getValue().booleanValue()) {
                    String v = BDDForVariables.bijectionIdAtom.atom(e.getKey().intValue());
                    if (v == null) {
                        strValuation = strValuation + "v" + e.getKey().intValue();
                    } else {
                        strValuation = strValuation + v;
                    }
                    first = false;
                } else {
                    String v = BDDForVariables.bijectionIdAtom.atom(e.getKey().intValue());
                    if (v == null) {
                        strValuation = strValuation + (e.getValue().booleanValue() ? ", v" + e.getKey().intValue() : "");
                    } else {
                        strValuation = strValuation + (e.getValue().booleanValue() ? ", " + v : "");;
                    }

                }
            }
            strValuation = strValuation + "}";
        }
        return strValuation;
    }

    public Formula toFormula() {
        Formula result = null;
        for (Map.Entry<Integer, Boolean> e : this.entrySet()) {
            Literal l = new Literal(BDDForVariables.bijectionIdAtom.atom(e.getKey()), e.getKey(), !e.getValue());
            if (result == null) {
                result = l;
            } else {
                result = new Conjunction(result, l);
            }
        }
        return result;
    }

 
    public BDD toValuationBDD() {
        BDD result = BDDForVariables.getTrueBDD();  // BDD for True
        for (Integer i : this.keySet()) {
            if (this.get(i)) {
                result = result.and(BDDForVariables.variableToBDD(i));
            } else {
                result = result.and(BDDForVariables.variableToBDD(i).not());
            }
        }
        return result;
    }

}
