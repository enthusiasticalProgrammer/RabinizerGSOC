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

import java.util.ArrayList;

public class AccTGR extends ArrayList<GRabinPair<TranSet<Product.ProductState>>> {

    private static final long serialVersionUID = 2472964991141498843L;

    public AccTGR(DTGRARaw.AccTGRRaw<Product.ProductState> accTGR) {
        for (GRabinPairRaw<Product.ProductState> grp : accTGR) {
            add(grp.order());
        }
    }

    @Override
    public String toString() {
        String result = "Gen. Rabin acceptance condition";
        int i = 1;
        for (GRabinPair<TranSet<Product.ProductState>> pair : this) {
            result += "\nPair " + i + "\n" + pair;
            i++;
        }
        return result;
    }
}
