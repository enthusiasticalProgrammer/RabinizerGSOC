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

import jhoafparser.ast.BooleanExpression;
import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import rabinizer.automata.output.HOAConsumerExtended;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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
    public void toHOA(HOAConsumer ho) throws HOAConsumerException {
        if (getStates().isEmpty()) {
            ho.notifyHeaderStart("v1");
            ho.setNumberOfStates(0);
            ho.setAPs(Collections.emptyList());
            ho.setAcceptanceCondition(0, new BooleanExpression<>(false));
            ho.notifyBodyStart();
            ho.notifyEnd();
            return;
        }

        HOAConsumerExtended<Product.ProductState> hoa = new HOAConsumerExtended<>(ho, HOAConsumerExtended.AutomatonType.TRANSITION);
        hoa.setHeader(null, valuationSetFactory.getAlphabet());
        hoa.setInitialState(this.initialState);
        hoa.setAcceptanceCondition(acc);

        for (Product.ProductState s : getStates()) {
            hoa.addState(s);

            for (Set<String> valuation : valuationSetFactory.createUniverseValuationSet()) {
                Product.ProductState successor = getSuccessor(s, valuation);

                BitSet accSet = new BitSet();

                acc.forEach(pair -> {
                    if (pair.fin.contains(s, valuation)) {
                        accSet.set(hoa.getNumber(pair.fin));
                    }

                    for (TranSet<Product.ProductState> inf : pair.infs) {
                        if (inf.contains(s, valuation)) {
                            accSet.set(hoa.getNumber(inf));
                        }
                    }
                });

                hoa.addEdge(s, valuation, successor, accSet);
            }

            hoa.stateDone();
        }

        hoa.done();
    }
}
