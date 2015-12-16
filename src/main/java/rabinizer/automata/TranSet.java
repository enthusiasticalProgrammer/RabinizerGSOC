package rabinizer.automata;

import rabinizer.ltl.ValuationSet;
import rabinizer.ltl.ValuationSetFactory;

import java.util.HashMap;

/**
 * @author jkretinsky
 */
public class TranSet<S extends IState<S>> extends HashMap<S, ValuationSet> {

    private static final long serialVersionUID = 1013653255527479470L;
    private final ValuationSetFactory<String> factory;

    public TranSet(ValuationSetFactory<String> factory) {
        this.factory = factory;
    }

    public TranSet<S> add(S s, ValuationSet vs) {
        if (!this.containsKey(s)) {
            this.put(s, vs);
        } else {
            ValuationSet old = factory.createValuationSet(this.get(s));
            old.addAll(vs);
            this.put(s, old);
        }
        return this;
    }

    public TranSet<S> addAll(TranSet<S> ts) {
        for (S s : ts.keySet()) {
            this.add(s, ts.get(s));
        }
        return this;
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
        for (S s : ts.keySet()) {
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

}
