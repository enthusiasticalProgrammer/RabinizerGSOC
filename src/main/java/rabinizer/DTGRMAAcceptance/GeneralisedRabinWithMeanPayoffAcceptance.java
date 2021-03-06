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

package rabinizer.DTGRMAAcceptance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jhoafparser.ast.AtomAcceptance;
import jhoafparser.ast.BooleanExpression;
import omega_automaton.AutomatonState;
import omega_automaton.acceptance.GeneralisedRabinAcceptance;
import omega_automaton.acceptance.OmegaAcceptance;
import omega_automaton.collections.TranSet;
import omega_automaton.collections.Tuple;
import omega_automaton.collections.valuationset.ValuationSet;
import rabinizer.automata.FrequencySelfProductSlave;
import rabinizer.automata.FrequencySelfProductSlave.State;
import rabinizer.automata.Product;

public class GeneralisedRabinWithMeanPayoffAcceptance extends GeneralisedRabinAcceptance<Product<FrequencySelfProductSlave.State>.ProductState> implements OmegaAcceptance {

    /**
     * The difference between this class and Generalised Rabin Acceptance is
     * that each "Rabin pair" of this class has also a list of MDP-rewards,
     * which are to be fullfilled. This is used by Prism
     */
    List<Collection<BoundAndReward>> acceptanceMDP;

    public GeneralisedRabinWithMeanPayoffAcceptance(
            List<Tuple<TranSet<Product<FrequencySelfProductSlave.State>.ProductState>, List<TranSet<Product<FrequencySelfProductSlave.State>.ProductState>>>> acceptance,
            List<Collection<BoundAndReward>> acceptanceMDP) {
        super(acceptance);
        this.acceptanceMDP = acceptanceMDP;
    }

    @Override
    public String getName() {
        return null; // HOA does not support our acceptance type
    }

    @Override
    public List<Object> getNameExtra() {
        return Collections.emptyList();
    }

    @Override
    public int getAcceptanceSets() {
        int size = super.getAcceptanceSets();
        for (Collection<BoundAndReward> set : acceptanceMDP) {
            for (BoundAndReward rew : set) {
                size += rew.getNumberOfRewardSets();
            }
        }
        return size;
    }

    @Override
    protected BooleanExpression<AtomAcceptance> addInfiniteSetsToConjunction(BooleanExpression<AtomAcceptance> conjunction, int offset) {
        conjunction = super.addInfiniteSetsToConjunction(conjunction, offset);
        // add some information about acceptanceMDP to the conjunction
        // the information, which we add here are necessary but not sufficient
        // for the real acceptance condition, which the hoa-Format does not
        // currently support.

        for (BoundAndReward reward : acceptanceMDP.get(offset)) {
            BooleanExpression<AtomAcceptance> disjunction = null;
            for (Entry<Integer, TranSet<Product<FrequencySelfProductSlave.State>.ProductState>> entry : reward.relevantEntries()) {
                BooleanExpression<AtomAcceptance> newSet = new BooleanExpression<>(new AtomAcceptance(AtomAcceptance.Type.TEMPORAL_INF, getTranSetId(entry.getValue()), false));
                disjunction = (disjunction == null ? newSet : disjunction.or(newSet));
            }
            if (disjunction != null) {
                conjunction = conjunction.and(disjunction);
            }
        }
        return conjunction;

    }

    @Override
    public Map<String, List<Object>> miscellaneousAnnotations() {
        Map<String, List<Object>> result = new HashMap<>();
        int i = 0;
        for (Collection<BoundAndReward> set : acceptanceMDP) {
            for (BoundAndReward bound : set) {
                String name = "boundary" + i++;
                List<Object> attributes = new ArrayList<>();
                StringBuilder attributeBuilder = new StringBuilder();
                attributeBuilder.append(bound.GOp.limes);
                attributeBuilder.append(bound.GOp.cmp);
                attributeBuilder.append(bound.GOp.bound);
                attributes.add(attributeBuilder.toString());
                attributes.add("   In this context, the following sets have rewards. ");
                Set<Entry<Integer, TranSet<Product<FrequencySelfProductSlave.State>.ProductState>>> entries = bound.relevantEntries();
                for (Entry<Integer, TranSet<Product<FrequencySelfProductSlave.State>.ProductState>> entry : entries) {
                    attributes.add("set(" + getTranSetId(entry.getValue()) + ")::" + entry.getKey());
                }
                result.put(name, attributes);
            }
        }
        return result;
    }

    @Override
    public void removeIndices(Set<Integer> indices) {
        super.removeIndices(indices);
        indices.stream().sorted(Collections.reverseOrder()).forEachOrdered(index -> acceptanceMDP.remove(index.intValue()));
    }

    @Override
    public void removeEach() {
        super.removeEach();
        acceptanceMDP.clear();
    }

    @Override
    public boolean implies(int premiseIndex, int conclusionIndex) {
        return super.implies(premiseIndex, conclusionIndex) && acceptanceMDP.get(premiseIndex).containsAll(acceptanceMDP.get(conclusionIndex));
    }

    public List<Collection<BoundAndReward>> getUnmodifiableAcceptanceMDP() {
        return Collections.unmodifiableList(this.acceptanceMDP);
    }

    @Override
    public Set<ValuationSet> getMaximallyMergedEdgesOfEdge(AutomatonState<?> currentState, ValuationSet initialValuation) {
        Set<ValuationSet> result = super.getMaximallyMergedEdgesOfEdge(currentState, initialValuation);

        for (Collection<BoundAndReward> boundSet : this.acceptanceMDP) {
            for (BoundAndReward bound : boundSet) {
                for (Entry<Integer, TranSet<Product<State>.ProductState>> entry : bound.relevantEntries()) {
                    result = splitAccordingToAcceptanceSet(currentState, result, entry.getValue());
                }
            }
        }

        return result;
    }
}
