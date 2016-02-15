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

import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import org.jetbrains.annotations.NotNull;
import rabinizer.collections.Tuple;
import rabinizer.collections.valuationset.ValuationSet;
import rabinizer.collections.valuationset.ValuationSetFactory;

/**
 * @author jkretinsky
 */
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
                if (valuationSetFactory.createAllValuationSets().equals(rp.left.get(s))) {
                    stateAcceptance.get(s).add(2 * i);
                } else if (valuationSetFactory.createAllValuationSets().equals(rp.right.get(s))) {
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
        return accSR.size() / 2;
    }

    @Override
    protected @NotNull ProductDegenAccState generateInitialState() {
        return new ProductDegenAccState(dtra.initialState, stateAcceptance.get(dtra.initialState));
    }

    public class ProductDegenAccState extends Tuple<DTRA.ProductDegenState, Set<Integer>> implements IState<ProductDegenAccState> {

        public ProductDegenAccState(DTRA.ProductDegenState pds, Set<Integer> accSets) {
            super(pds, accSets);
        }

        @Override
        public String toString() {
            String result = left.toString();
            int[] orderedSets = new int[right.size()];
            int i = 0;
            for (Integer set : right) {
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
        public ProductDegenAccState getSuccessor(@NotNull Set<String> valuation) {
            DTRA.ProductDegenState succ = dtra.getSuccessor(left, valuation);
            Set<Integer> accSets = new HashSet<>(stateAcceptance.get(succ));
            for (int i = 0; i < dtra.accTR.size(); i++) {
                RabinPair<? extends IState<?>> rp = dtra.accTR.get(i);
                if (rp.left != null && rp.left.get(left) != null
                        && rp.left.get(left).contains(valuation) && !stateAcceptance.get(left).contains(2 * i)) {
                    // acceptance
                    // dealt
                    // with
                    // already
                    // in s
                    accSets.add(2 * i);
                }
                if (rp.right != null && rp.right.get(left) != null
                        && rp.right.get(left).contains(valuation)
                        && !stateAcceptance.get(left).contains(2 * i + 1)) {
                    accSets.add(2 * i + 1);
                }
                if (accSets.contains(2 * i) && accSets.contains(2 * i + 1)) {
                    accSets.remove(2 * i + 1);
                }
            }
            return new ProductDegenAccState(succ, accSets);
        }

        @Override
        public @NotNull Set<ValuationSet> partitionSuccessors() {
            return valuationSetFactory.createAllValuationSets(); // TODO symbolic
        }

        @Override
        public @NotNull Set<String> getSensitiveAlphabet() {
            return valuationSetFactory.getAlphabet();
        }

        @Override
        public @NotNull ValuationSetFactory getFactory() {
            return valuationSetFactory;
        }
    }

    @Override
    public void toHOA(HOAConsumer hoa) throws HOAConsumerException {
        throw new UnsupportedOperationException();
    }

    /**
     * @author jkretinsky
     */
    public static class AccSR extends ArrayList<Set<ProductDegenAccState>> {

        private static final long serialVersionUID = 1L;

        AccSR(int size, DSRA dsra) {
            for (int i = 0; i < 2 * size; i++) {
                this.add(new HashSet<>());
            }

            for (ProductDegenAccState s : dsra.states) {
                for (Integer i : s.right) {
                    this.get(i).add(s);
                }
            }
        }

        @Override
        public String toString() {
            String result = "Rabin state-based acceptance condition";
            for (int i = 0; i < size() / 2; i++) {
                result += "\nPair " + (i + 1) + "\nFin:\n" + get(2 * i) + "\nInf:\n" + get(2 * i + 1);
            }
            return result;
        }
    }
}
