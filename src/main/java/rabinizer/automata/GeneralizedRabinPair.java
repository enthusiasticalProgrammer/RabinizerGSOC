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

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class GeneralizedRabinPair<S> {

    public final @NotNull TranSet<S> fin;
    public final @NotNull List<@NotNull TranSet<S>> infs;

    public GeneralizedRabinPair(@NotNull TranSet<S> l, @NotNull List<@NotNull TranSet<S>> r) {
        this.fin = l;
        this.infs = r;
    }

    public GeneralizedRabinPair(RabinPair<S> pair) {
        this.fin = pair.fin;
        this.infs = Collections.singletonList(pair.inf);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GeneralizedRabinPair<?> tuple = (GeneralizedRabinPair<?>) o;
        return Objects.equals(fin, tuple.fin) &&
                Objects.equals(infs, tuple.infs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fin, infs);
    }

    @Override
    public String toString() {
        String result = "Fin:\n" + (fin) + "\nInf: ";
        if (infs.isEmpty()) {
            result += "0\ntrivial";
        } else {
            result += infs.size();
            for (TranSet<S> inf : infs) {
                result += "\n" + inf;
            }
        }
        return result;
    }
}
