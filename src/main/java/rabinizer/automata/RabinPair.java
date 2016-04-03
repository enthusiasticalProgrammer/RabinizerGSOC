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


import java.util.Objects;

public class RabinPair<S> {

    public final TranSet<S> fin;
    public final TranSet<S> inf;

    public RabinPair(TranSet<S> fin, TranSet<S> inf) {
        this.fin = fin;
        this.inf = inf;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RabinPair<?> rabinPair = (RabinPair<?>) o;
        return Objects.equals(fin, rabinPair.fin) &&
                Objects.equals(inf, rabinPair.inf);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fin, inf);
    }

    @Override
    public String toString() {
        return "Fin:\n" + (fin) + "\nInf:\n" + (inf);
    }
}