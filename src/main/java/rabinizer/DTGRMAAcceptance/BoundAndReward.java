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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.Set;

import ltl.FrequencyG;
import omega_automaton.collections.TranSet;
import omega_automaton.collections.valuationset.ValuationSet;
import omega_automaton.collections.valuationset.ValuationSetFactory;
import rabinizer.automata.FrequencySelfProductSlave;
import rabinizer.automata.FrequencySelfProductSlave.State;
import rabinizer.automata.Product;

/**
 * This class aims to store a bound and the transition, which have a reward
 * regarding this bound. Note that some transitions may have a reward larger
 * than one. An invariant: the TranSets as values of the Map must not intersect.
 */
public class BoundAndReward {

    private final ValuationSetFactory valuationSetFactory;

    /**
     * GOp is used by Prism
     */
    public final FrequencyG GOp;
    private final Map<Integer, TranSet<Product<FrequencySelfProductSlave.State>.ProductState>> reward;

    public BoundAndReward(FrequencyG GOp, ValuationSetFactory valuationSetFactory) {
        this.GOp = GOp;
        this.reward = new HashMap<>();
        this.valuationSetFactory = valuationSetFactory;
    }

    public void increaseRewards(Map<TranSet<Product<FrequencySelfProductSlave.State>.ProductState>, Integer> transitionRewards) {
        transitionRewards.entrySet().forEach(entry -> {
            if (!entry.getValue().equals(0))
                addRewards(entry.getKey(), entry.getValue());
        });
    }

    /**
     * Increases the reward of the input-transitions by <amount>.
     */
    private void addRewards(TranSet<Product<FrequencySelfProductSlave.State>.ProductState> trans, int amount) {
        final Integer zero = Integer.valueOf(0);
        Set<TranSet<Product<FrequencySelfProductSlave.State>.ProductState>> transitionSplitted = splitIntoRelevantJunks(trans);
        Map<Integer, TranSet<Product<FrequencySelfProductSlave.State>.ProductState>> toRemove = new HashMap<>();
        Map<Integer, TranSet<Product<FrequencySelfProductSlave.State>.ProductState>> toAdd = new HashMap<>();

        // find out the new rewards
        for (TranSet<Product<FrequencySelfProductSlave.State>.ProductState> singleSet : transitionSplitted) {
            reward.put(zero, singleSet);
            for (Map.Entry<Integer, TranSet<Product<FrequencySelfProductSlave.State>.ProductState>> entry : reward.entrySet()) {
                if (entry.getValue().containsAll(singleSet)) {
                    if (!entry.getKey().equals(zero)) {
                        toRemove.put(entry.getKey(), singleSet);
                    }
                    toAdd.put(entry.getKey() + amount, singleSet);
                    break;
                }
            }
            reward.remove(zero);
        }

        // adjust the rewards
        for (Entry<Integer, TranSet<Product<FrequencySelfProductSlave.State>.ProductState>> entry : toRemove.entrySet()) {
            TranSet<Product<FrequencySelfProductSlave.State>.ProductState> temporary = reward.get(entry.getKey()).copy();
            temporary.removeAll(entry.getValue());
            reward.put(entry.getKey(), temporary);
        }
        for (Entry<Integer, TranSet<Product<FrequencySelfProductSlave.State>.ProductState>> entry : toAdd.entrySet()) {
            TranSet<Product<FrequencySelfProductSlave.State>.ProductState> temporary = reward.get(entry.getKey());
            if (temporary == null) {
                reward.put(entry.getKey(), entry.getValue());
            } else {
                temporary.addAll(entry.getValue());
                reward.put(entry.getKey(), temporary);
            }
        }
    }

    private Set<TranSet<Product<FrequencySelfProductSlave.State>.ProductState>> splitIntoRelevantJunks(TranSet<Product<FrequencySelfProductSlave.State>.ProductState> trans) {
        Set<TranSet<Product<FrequencySelfProductSlave.State>.ProductState>> result = new HashSet<>();
        for (Entry<Integer, TranSet<Product<FrequencySelfProductSlave.State>.ProductState>> entry : reward.entrySet()) {
            if (entry.getValue().intersects(trans)) {
                TranSet<Product<FrequencySelfProductSlave.State>.ProductState> temp = new TranSet<>(valuationSetFactory);
                entry.getValue().forEach(singleState -> {
                    Map<Product<State>.ProductState, ValuationSet> transitionMap = trans.asMap();
                    if (transitionMap.containsKey(singleState)) {
                        temp.addAll(singleState.getKey(), singleState.getValue().intersect(transitionMap.get(singleState.getKey())));
                    }

                });
                if (!temp.isEmpty()) {
                    result.add(temp);
                    trans.removeAll(temp);
                }
            }
        }
        result.forEach(set -> trans.removeAll(set));
        if (!trans.isEmpty()) {
            result.add(trans);
        }
        return result;
    }

    public int getNumberOfRewardSets() {
        return reward.keySet().size();
    }

    /**
     * used by Prism
     */
    public Set<Entry<Integer, TranSet<Product<FrequencySelfProductSlave.State>.ProductState>>> relevantEntries() {
        if (reward.entrySet().isEmpty()) {
            reward.put(0, new TranSet<>(valuationSetFactory));
            HashMap<Integer, TranSet<Product<FrequencySelfProductSlave.State>.ProductState>> result = new HashMap<>(reward);
            reward.clear();
            return result.entrySet();
        }
        HashMap<Integer, TranSet<Product<FrequencySelfProductSlave.State>.ProductState>> result = new HashMap<>(reward);
        return Collections.unmodifiableSet(result.entrySet());
    }

    @Override
    public int hashCode() {
        return Objects.hash(GOp, reward);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BoundAndReward) {
            BoundAndReward that = (BoundAndReward) obj;
            return Objects.equals(this.GOp, that.GOp) && Objects.equals(this.reward, that.reward);
        }
        return false;

    }
}
