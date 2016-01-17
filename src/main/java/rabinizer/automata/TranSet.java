package rabinizer.automata;

import rabinizer.ltl.ValuationSet;
import rabinizer.ltl.ValuationSetFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author jkretinsky
 */
public class TranSet<S extends IState<S>> extends HashMap<S, ValuationSet> {

    private static final long serialVersionUID = 1013653255527479470L;
    private final ValuationSetFactory factory;

    public TranSet(ValuationSetFactory factory) {
        this.factory = factory;
    }

    public void add(S s, ValuationSet vs) {
        if (!this.containsKey(s)) {
            this.put(s, vs);
        } else {
            ValuationSet old = this.get(s).clone();
            old.addAll(vs);
            this.put(s, old);
        }
    }

    public void addAll(TranSet<S> ts) {
        for (Entry<S, ValuationSet> sValuationSetEntry : ts.entrySet()) {
            this.add(sValuationSetEntry.getKey(), sValuationSetEntry.getValue());
        }
    }

    @Override
    public String toString() {
        String result = "{";
        boolean first = true;
        for (IState s : this.keySet()) {
            result += (first ? "" : ";\n") + s + "  ->  " + get(s);
            first = false;
        }
        return result + "}";
    }

    boolean subsetOf(TranSet<S> ts) {
        for (IState s : this.keySet()) {
            if (!ts.containsKey(s) || !ts.get(s).contains(this.get(s))) {
                return false;
            }
        }
        return true;
    }

    void removeAll(TranSet<S> ts) {
        for (Entry<S, ValuationSet> sValuationSetEntry : ts.entrySet()) {
            if (this.containsKey(sValuationSetEntry.getKey())) {
                ValuationSet old = get(sValuationSetEntry.getKey()).clone();
                old.remove(sValuationSetEntry.getValue());
                this.put(sValuationSetEntry.getKey(), old);
                if (this.get(sValuationSetEntry.getKey()).isEmpty()) {
                    this.remove(sValuationSetEntry.getKey());
                }
            }
        }
    }

}
