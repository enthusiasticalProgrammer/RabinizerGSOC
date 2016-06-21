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

import javax.annotation.Nullable;

import com.google.common.collect.BiMap;

import jhoafparser.consumer.HOAConsumer;
import omega_automaton.Automaton;
import omega_automaton.acceptance.GeneralisedRabinAcceptance;
import omega_automaton.output.HOAConsumerExtended;
import omega_automaton.output.HOAConsumerGeneralisedRabin;
import rabinizer.automata.Product.ProductState;

import java.util.Collections;

public class DTGRA extends Automaton<Product.ProductState, GeneralisedRabinAcceptance<ProductState>> {

    public DTGRA(Product product, @Nullable GeneralisedRabinAcceptance<ProductState> acc) {
        super(product);

        if (acc != null) {
            this.acceptance = acc;
        } else {
            this.acceptance = new GeneralisedRabinAcceptance<>(Collections.emptyList());
        }
    }

    @Override
    protected HOAConsumerExtended getHOAConsumer(HOAConsumer ho, BiMap<String, Integer> aliases) {
        return new HOAConsumerGeneralisedRabin(ho, valuationSetFactory, aliases, initialState, acceptance, size());
    }
}