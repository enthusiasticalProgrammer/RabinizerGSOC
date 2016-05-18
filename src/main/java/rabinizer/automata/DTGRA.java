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

import jhoafparser.consumer.HOAConsumer;
import rabinizer.automata.output.HOAConsumerGeneralisedRabin;
import rabinizer.automata.output.HOAConsumerExtended;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class DTGRA extends Automaton<Product.ProductState> {

    @Nonnull
    final List<GeneralizedRabinPair<Product.ProductState>> acc;

    public DTGRA(Product product, @Nullable List<GeneralizedRabinPair<Product.ProductState>> acc) {
        super(product);

        if (acc != null) {
            this.acc = acc;
        } else {
            this.acc = Collections.emptyList();
        }
    }

    @Override
    public HOAConsumerExtended getConsumer(HOAConsumer ho) {
        return new HOAConsumerGeneralisedRabin(ho, valuationSetFactory, acc);
    }
}
