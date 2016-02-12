/*
 * Copyright (C) 2016  (See AUTHORS)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package rabinizer.automata;

import rabinizer.collections.valuationset.ValuationSet;
import rabinizer.collections.valuationset.ValuationSetFactory;

import java.util.HashMap;

/**
 * @author jkretinsky
 */
public class TranSet<S> extends HashMap<S, ValuationSet> {

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
        for (S s : this.keySet()) {
            result += (first ? "" : ";\n") + s + "  ->  " + get(s);
            first = false;
        }
        return result + "}";
    }

    boolean subsetOf(TranSet<S> ts) {
        for (S s : this.keySet()) {
            if (!ts.containsKey(s) || !ts.get(s).containsAll(this.get(s))) {
                return false;
            }
        }
        return true;
    }

    void removeAll(TranSet<S> ts) {
        for (Entry<S, ValuationSet> sValuationSetEntry : ts.entrySet()) {
            if (this.containsKey(sValuationSetEntry.getKey())) {
                ValuationSet old = get(sValuationSetEntry.getKey()).clone();
                old.removeAll(sValuationSetEntry.getValue());
                this.put(sValuationSetEntry.getKey(), old);
                if (this.get(sValuationSetEntry.getKey()).isEmpty()) {
                    this.remove(sValuationSetEntry.getKey());
                }
            }
        }
    }

    @Override
    public TranSet<S> clone() {
        TranSet<S> result = new TranSet<>(factory);
        result.addAll(this);
        return result;
    }
}
