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
import rabinizer.ltl.Formula;
import rabinizer.ltl.Literal;
import rabinizer.ltl.equivalence.EquivalenceClass;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public abstract class AbstractFormulaState {

    protected final EquivalenceClass clazz;

    protected AbstractFormulaState(EquivalenceClass clazz) {
        this.clazz = clazz;
    }

    @Override
    public String toString() {
        return clazz.getRepresentative().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AbstractFormulaState that = (AbstractFormulaState) o;
        return Objects.equals(this.getOuter(), that.getOuter()) && Objects.equals(clazz, that.clazz);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clazz);
    }

    public EquivalenceClass getClazz() {
        return clazz;
    }

    protected abstract Object getOuter();

    protected Set<String> getSensitive(boolean unfoldG) {
        Set<String> letters = new HashSet<>();

        for (Formula literal : clazz.unfold(unfoldG).getSupport()) {
            if (literal instanceof Literal) {
                letters.add(((Literal) literal).atom);
            }
        }

        return letters;
    }
}
