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

import rabinizer.collections.Tuple;

import java.util.Collection;
import java.util.List;

public class GRabinPair<S> extends Tuple<S, List<S>> {

    public GRabinPair(S l, List<S> r) {
        super(l, r);
    }

    @Override
    public String toString() {
        String result = "Fin:\n" + (left == null ? "trivial" : left) + "\nInf: ";
        if (right == null || right.isEmpty()) {
            result += "0\ntrivial";
        } else {
            result += right.size();
            for (S inf : right) {
                result += "\n" + inf;
            }
        }
        return result;
    }

}
