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

import com.google.common.collect.Table;
import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rabinizer.automata.output.HOAConsumerExtended;
import rabinizer.collections.Tuple;
import rabinizer.collections.valuationset.ValuationSet;
import rabinizer.collections.valuationset.ValuationSetFactory;

import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DSRA extends Automaton<DSRA.ProductDegenAccState> implements AccAutomatonInterface {

    public AccSR accSR;
    DTRA dtra;
    Map<DTRA.ProductDegenState, BitSet> stateAcceptance;

    public DSRA(DTRA dtra) {
        super(dtra.valuationSetFactory);
        this.dtra = dtra;
        trapState = new ProductDegenAccState(dtra.trapState, new BitSet());
        stateAcceptance = new HashMap<>();

        for (DTRA.ProductDegenState s : dtra.getStates()) {
            stateAcceptance.put(s, new BitSet());

            for (int i = 0; i < dtra.accTR.size(); i++) {
                RabinPair<DTRA.ProductDegenState> rp = dtra.accTR.get(i);

                if (valuationSetFactory.createUniverseValuationSet().equals(rp.left.get(s))) {
                    stateAcceptance.get(s).set(2 * i);
                } else if (valuationSetFactory.createUniverseValuationSet().equals(rp.right.get(s))) {
                    stateAcceptance.get(s).set(2 * i + 1);
                }
            }
        }

        generate();
        accSR = new AccSR(dtra.accTR.size(), this);
    }

    @Override
    public void acc(PrintStream p) {
        p.print(accSR);
    }

    @Override
    public int pairNumber() {
        return accSR.acc.size();
    }

    @Override
    public void toHOA(HOAConsumer ho) throws HOAConsumerException {
        HOAConsumerExtended<DSRA.ProductDegenAccState> hoa = new HOAConsumerExtended<>(ho, HOAConsumerExtended.AutomatonType.STATE);
        hoa.setHeader(null, valuationSetFactory.getAlphabet());
        hoa.setInitialState(this.initialState);
        hoa.setAcceptanceCondition(accSR.acc);

        for (ProductDegenAccState s : states) {
            List<Integer> stAccSetIds = new ArrayList<>();
            IntStream.range(0, accSR.acc.size()).filter(i -> (accSR.acc.get(i).left).contains(s)).forEach(i -> stAccSetIds.add(hoa.getNumber(accSR.acc.get(i).left)));
            IntStream.range(0, accSR.acc.size()).filter(i -> (accSR.acc.get(i).right).contains(s)).forEach(i -> stAccSetIds.add(hoa.getNumber(accSR.acc.get(i).right)));
            hoa.addState(s, stAccSetIds);
            for (Table.Cell<ProductDegenAccState, ValuationSet, ProductDegenAccState> trans : transitions.cellSet()) {
                if (trans.getRowKey().equals(s)) {
                    hoa.addEdge(trans.getRowKey(), trans.getColumnKey().toFormula(), trans.getValue());
                }
            }
        }

        hoa.done();
    }

    @Override
    protected @NotNull ProductDegenAccState generateInitialState() {
        return new ProductDegenAccState(dtra.initialState, stateAcceptance.get(dtra.initialState));
    }

    public static class AccSR {
        /**
         * This represents the state-based acceptance, left is Fin, right is
         * Inf.
         */
        public final List<Tuple<? extends Set<ProductDegenAccState>, ? extends Set<ProductDegenAccState>>> acc;

        AccSR(int size, DSRA dsra) {
            acc = IntStream.range(0, size).mapToObj(i -> new Tuple<>(new HashSet<ProductDegenAccState>(), new HashSet<ProductDegenAccState>()))
                    .collect(Collectors.toList());

            for (ProductDegenAccState s : dsra.states) {
                for (int i = s.accSet.nextSetBit(0); i >= 0; i = s.accSet.nextSetBit(i + 1)) {
                    if (i % 2 == 0) {
                        acc.get(i / 2).left.add(s);
                    } else {
                        acc.get(i / 2).right.add(s);
                    }

                    // operate on index i here
                    if (i == Integer.MAX_VALUE) {
                        break; // or (i+1) would overflow
                    }
                }
            }
        }

        @Override
        public String toString() {
            String result = "Rabin state-based acceptance condition";
            for (Tuple<? extends Set<ProductDegenAccState>, ? extends Set<ProductDegenAccState>> pair : acc) {
                result += "\nPair " + "\nFin:\n" + pair.left + "\nInf:\n" + pair.right;
            }
            return result;
        }
    }

    public class ProductDegenAccState implements IState<ProductDegenAccState> {

        private final DTRA.ProductDegenState productDegenState;
        private final BitSet accSet;

        public ProductDegenAccState(@NotNull DTRA.ProductDegenState pds, @NotNull BitSet accSet) {
            this.productDegenState = pds;
            this.accSet = accSet;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(productDegenState.toString());

            for (int i = accSet.nextSetBit(0); i >= 0; i = accSet.nextSetBit(i + 1)) {
                builder.append(' ').append((i % 2) != 1 ? '-' : '+').append(i / 2 + 1);

                // operate on index i here
                if (i == Integer.MAX_VALUE) {
                    break; // or (i+1) would overflow
                }
            }

            return builder.toString();
        }

        @Override
        public @Nullable ProductDegenAccState getSuccessor(@NotNull Set<String> valuation) {
            DTRA.ProductDegenState succ = dtra.getSuccessor(productDegenState, valuation);

            if (succ == null) {
                return null;
            }

            BitSet accSets;

            if (stateAcceptance.get(succ) == null) {
                accSets = new BitSet();
            } else {
                accSets = (BitSet) stateAcceptance.get(succ).clone();
            }

            // TODO: foreach loop
            for (int i = 0; i < dtra.accTR.size(); i++) {
                RabinPair<DTRA.ProductDegenState> rp = dtra.accTR.get(i);

                // acceptance dealt with already in s
                if (rp.left != null && rp.left.get(productDegenState) != null
                        && rp.left.get(productDegenState).contains(valuation) && !stateAcceptance.get(productDegenState).get(2 * i)) {
                    accSets.set(2 * i);
                }
                if (rp.right != null && rp.right.get(productDegenState) != null
                        && rp.right.get(productDegenState).contains(valuation)
                        && !stateAcceptance.get(productDegenState).get(2 * i + 1)) {
                    accSets.set(2 * i + 1);
                }

                if (accSets.get(2 * i)) {
                    accSets.clear(2 * i + 1);
                }
            }

            return new ProductDegenAccState(succ, accSets);
        }

        @Override
        public @NotNull Set<ValuationSet> partitionSuccessors() {
            return valuationSetFactory.createAllValuationSets();
        }

        @Override
        public @NotNull Set<String> getSensitiveAlphabet() {
            return valuationSetFactory.getAlphabet();
        }

        @Override
        public @NotNull ValuationSetFactory getFactory() {
            return valuationSetFactory;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ProductDegenAccState tuple = (ProductDegenAccState) o;
            return Objects.equals(productDegenState, tuple.productDegenState) &&
                    Objects.equals(accSet, tuple.accSet);
        }

        @Override
        public int hashCode() {
            return Objects.hash(productDegenState, accSet);
        }
    }
}
