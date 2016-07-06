package rabinizer.DTGRMAAcceptance;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import ltl.FrequencyG;
import omega_automaton.collections.TranSet;
import omega_automaton.collections.valuationset.ValuationSetFactory;
import rabinizer.automata.Product;
import rabinizer.automata.Product.ProductState;

/**
 * This class aims to store a bound and the transition, which have a reward
 * regarding this bound. Note that some transitions may have a reward larger
 * than one. An invariant: the TranSets as values of the Map must not intersect.
 */
public class BoundAndReward {

    private final ValuationSetFactory valuationSetFactory;
    protected final FrequencyG GOp;
    private final Map<Integer, TranSet<Product.ProductState<?>>> reward;

    public BoundAndReward(FrequencyG GOp, ValuationSetFactory valuationSetFactory) {
        this.GOp = GOp;
        this.reward = new HashMap<>();
        this.valuationSetFactory = valuationSetFactory;
    }

    public void increaseRewards(Map<TranSet<ProductState<?>>, Integer> transitionRewards) {
        transitionRewards.entrySet().forEach(entry -> {
            if (!entry.getValue().equals(0))
                addRewards(entry.getKey(), entry.getValue());
        });
    }

    /**
     * Increases the reward of the input-transitions by <amount>.
     */
    private void addRewards(TranSet<ProductState<?>> trans, int amount) {
        Set<TranSet<ProductState<?>>> transitionSplitted = splitIntoRelevantJunks(trans);
        Map<Integer, TranSet<ProductState<?>>> toRemove = new HashMap<>();
        Map<Integer, TranSet<ProductState<?>>> toAdd = new HashMap<>();

        // find out the new rewards
        for (TranSet<ProductState<?>> singleSet : transitionSplitted) {
            reward.put(0, singleSet);
            for (Map.Entry<Integer, TranSet<ProductState<?>>> entry : reward.entrySet()) {
                if (entry.getValue().containsAll(singleSet)) {
                    if (entry.getKey() != 0) {
                        toRemove.put(entry.getKey(), singleSet);
                    }
                    toAdd.put(entry.getKey() + amount, singleSet);
                    break;
                }
            }
            reward.remove(0);

        }

        // adjust the rewards
        for (Entry<Integer, TranSet<ProductState<?>>> entry : toRemove.entrySet()) {
            TranSet<ProductState<?>> temporary = reward.get(entry.getKey());
            temporary.removeAll(entry.getValue());
            reward.put(entry.getKey(), temporary);
        }
        for (Entry<Integer, TranSet<ProductState<?>>> entry : toAdd.entrySet()) {
            TranSet<ProductState<?>> temporary = reward.get(entry.getKey());
            if (temporary == null) {
                reward.put(entry.getKey(), entry.getValue());
            } else {
                temporary.addAll(entry.getValue());
                reward.put(entry.getKey(), temporary);
            }
        }
    }

    private Set<TranSet<ProductState<?>>> splitIntoRelevantJunks(TranSet<ProductState<?>> trans) {
        Set<TranSet<ProductState<?>>> result = new HashSet<>();
        for (Entry<Integer, TranSet<ProductState<?>>> entry : reward.entrySet()) {
            if (entry.getValue().intersects(trans)) {
                TranSet<ProductState<?>> temp = new TranSet<>(valuationSetFactory);
                entry.getValue().forEach(singleState -> {
                    if (trans.asMap().containsKey(singleState)) {
                        temp.addAll(singleState.getKey(), singleState.getValue().intersect(trans.asMap().get(singleState.getKey())));
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

    public Set<Entry<Integer, TranSet<ProductState<?>>>> relevantEntries() {
        HashMap<Integer, TranSet<Product.ProductState<?>>> result = new HashMap<>(reward);
        return result.entrySet();
    }
}
