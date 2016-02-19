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

import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Table;

import rabinizer.automata.output.HOAConsumerExtended;
import rabinizer.collections.Tuple;
import rabinizer.collections.valuationset.ValuationSet;
import rabinizer.collections.valuationset.ValuationSetFactory;

public class DSRA extends Automaton<DSRA.ProductDegenAccState> implements AccAutomatonInterface {

    public AccSR accSR;
    DTRA dtra;
    Map<DTRA.ProductDegenState, Set<Integer>> stateAcceptance;

    public DSRA(DTRA dtra) {
        super(dtra.valuationSetFactory);
        this.dtra = dtra;
        trapState = new ProductDegenAccState(dtra.trapState, new HashSet<>());
        stateAcceptance = new HashMap<>();
        for (DTRA.ProductDegenState s : dtra.getStates()) {
            stateAcceptance.put(s, new HashSet<>());
            for (int i = 0; i < dtra.accTR.size(); i++) {
                RabinPair<? extends IState<?>> rp = dtra.accTR.get(i);
                if (valuationSetFactory.createUniverseValuationSet().equals(rp.left.get(s))) {
                    stateAcceptance.get(s).add(2 * i);
                } else if (valuationSetFactory.createUniverseValuationSet().equals(rp.right.get(s))) {
                    stateAcceptance.get(s).add(2 * i + 1);
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
    protected @NotNull ProductDegenAccState generateInitialState() {
        return new ProductDegenAccState(dtra.initialState, stateAcceptance.get(dtra.initialState));
    }

    public class ProductDegenAccState implements IState<ProductDegenAccState> {

        final DTRA.ProductDegenState productDegenState;
        // TODO: Replace by BitSet
        final Set<Integer> accSet;

        public ProductDegenAccState(@NotNull DTRA.ProductDegenState pds, @NotNull Set<Integer> accSet) {
            this.productDegenState = pds;
            this.accSet = accSet;
        }

        @Override
        public String toString() {
            String result = productDegenState.toString();
            int[] orderedSets = new int[accSet.size()];
            int i = 0;
            for (Integer set : accSet) {
                orderedSets[i] = set;
                i++;
            }
            Arrays.sort(orderedSets);
            for (i = 0; i < orderedSets.length; i++) {
                int j = orderedSets[i];
                result += " " + (j % 2 == 1 ? "+" : "-") + (j / 2 + 1);
            }
            return result;
        }

        @Override
        public @Nullable ProductDegenAccState getSuccessor(@NotNull Set<String> valuation) {
            DTRA.ProductDegenState succ = dtra.getSuccessor(productDegenState, valuation);

            if (succ == null) {
                return null;
            }

            Set<Integer> accSets;

            if (stateAcceptance.get(succ) == null) {
                accSets = new HashSet<>();
            } else {
                accSets = new HashSet<>(stateAcceptance.get(succ));
            }

            // TODO: foreach loop
            for (int i = 0; i < dtra.accTR.size(); i++) {
                RabinPair<? extends IState<?>> rp = dtra.accTR.get(i);

                // acceptance dealt with already in s
                if (rp.left != null && rp.left.get(productDegenState) != null
                        && rp.left.get(productDegenState).contains(valuation) && !stateAcceptance.get(productDegenState).contains(2 * i)) {
                    accSets.add(2 * i);
                }
                if (rp.right != null && rp.right.get(left) != null
                        && rp.right.get(left).contains(valuation)
                        && !stateAcceptance.get(left).contains(2 * i + 1)) {
                    accSets.add(2 * i + 1);
                }

                if (accSets.contains(2 * i)) {
                    accSets.remove(2 * i + 1);
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

    @Override
    public void toHOA(HOAConsumer ho) throws HOAConsumerException {
        HOAConsumerExtended<DSRA.ProductDegenAccState> hoa = new HOAConsumerExtended<>(ho, HOAConsumerExtended.AutomatonType.STATE);
        hoa.setHeader(null, valuationSetFactory.getAlphabet());
        hoa.setInitialState(this.initialState);
        hoa.setAcceptanceCondition(accSR.acc);


        for (ProductDegenAccState s : states) {
            List<Integer> stAccSetIds = new ArrayList<Integer>();
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


    public static class AccSR {

        /**
         * This represents the state-based acceptance, left is Fin, right is
         * Inf.
         */
        public final List<Tuple<? extends Set<ProductDegenAccState>, ? extends Set<ProductDegenAccState>>> acc;

        AccSR(int size, DSRA dsra) {
            acc = IntStream.range(0, size).mapToObj(i -> new Tuple(new HashSet<ProductDegenAccState>(), new HashSet<ProductDegenAccState>()))
                    .collect(Collectors.toList());

            for (ProductDegenAccState s : dsra.states) {
                for (Integer i : s.right) {
                    (i % 2 == 0 ? this.acc.get(i / 2).left : this.acc.get(i / 2).right).add(s);
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
}
