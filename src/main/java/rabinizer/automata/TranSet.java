/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rabinizer.automata;

import rabinizer.ltl.ValuationSet;
import rabinizer.ltl.ValuationSetFactory;

import java.util.HashMap;

/**
 * @author jkretinsky
 */
public class TranSet<State> extends HashMap<State, ValuationSet> {

    /**
     *
     */
    private static final long serialVersionUID = 1013653255527479470L;
    private final ValuationSetFactory<String> factory;

    public TranSet(ValuationSetFactory<String> factory) {
        this.factory = factory;
    }

    public TranSet<State> add(State s, ValuationSet vs) {
        if (!this.containsKey(s)) {
            this.put(s, vs);
        } else {
            ValuationSet old = factory.createValuationSet(this.get(s));
            old.addAll(vs);
            this.put(s, old);
        }
        return this;
    }

    public TranSet<State> addAll(TranSet<State> ts) {
        for (State s : ts.keySet()) {
            this.add(s, ts.get(s));
        }
        return this;
    }

    boolean subsetOf(TranSet<State> ts) {
        for (State s : this.keySet()) {
            if (!ts.containsKey(s) || !ts.get(s).contains(this.get(s))) {
                return false;
            }
        }
        return true;
    }

    void removeAll(TranSet<State> ts) {
        for (State s : ts.keySet()) {
            if (this.containsKey(s)) {
                ValuationSet old = factory.createValuationSet(this.get(s));
                old.remove(ts.get(s));
                this.put(s, old);
                if (this.get(s).isEmpty()) {
                    this.remove(s);
                }
            }
        }
    }

    public String toString() {
        String result = "{";
        boolean first = true;
        for (State s : this.keySet()) {
            result += (first ? "" : ";\n") + s + "  ->  " + get(s);
            first = false;
        }
        return result + "}";
    }

}
