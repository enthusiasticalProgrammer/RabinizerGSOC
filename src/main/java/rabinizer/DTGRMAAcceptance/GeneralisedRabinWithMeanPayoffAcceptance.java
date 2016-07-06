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
import omega_automaton.acceptance.GeneralisedRabinAcceptance;
import omega_automaton.acceptance.OmegaAcceptance;
import omega_automaton.collections.TranSet;
import omega_automaton.collections.Tuple;
import rabinizer.automata.Product.ProductState;

public class GeneralisedRabinWithMeanPayoffAcceptance extends GeneralisedRabinAcceptance<ProductState<?>> implements OmegaAcceptance {

    /**
     * The difference between this class and Generalised Rabin Acceptance is
     * that each "Rabin pair" of this class has also a list of MDP-rewards,
     * which are to be fullfilled.
     */
    public List<Collection<BoundAndReward>> acceptanceMDP;

    public GeneralisedRabinWithMeanPayoffAcceptance(List<Tuple<TranSet<ProductState<?>>, List<TranSet<ProductState<?>>>>> acceptance,
            List<Collection<BoundAndReward>> acceptanceMDP) {
        super(acceptance);
        this.acceptanceMDP = acceptanceMDP;
    }

    @Override
    public String getName() {
        return ""; // HOA does not support our acceptance type
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
    protected BooleanExpression<AtomAcceptance> addInfiniteSetsToConjunction(BooleanExpression<AtomAcceptance> conjunction,
            Tuple<TranSet<ProductState<?>>, List<TranSet<ProductState<?>>>> pair) {
        conjunction = super.addInfiniteSetsToConjunction(conjunction, pair);

        // add some information about acceptanceMDP to the conjunction
        // the information, which we add here are necessary but not sufficient
        // for the real acceptance condition, which the hoa-Format does not
        // currently support.

        int offset = acceptanceCondition.indexOf(pair);
        for (BoundAndReward reward : acceptanceMDP.get(offset)) {
            BooleanExpression<AtomAcceptance> disjunction = null;
            for (Entry<Integer, TranSet<ProductState<?>>> entry : reward.relevantEntries()) {
                BooleanExpression<AtomAcceptance> newSet = new BooleanExpression<>(new AtomAcceptance(AtomAcceptance.Type.TEMPORAL_INF, getTranSetId(entry.getValue()), false));
                disjunction = (disjunction == null ? newSet : disjunction.or(newSet));
            }

            conjunction = conjunction.and(disjunction);
        }
        return conjunction;

    }

    @Override
    public Map<String, List<Object>> miscellaneousAnnotations() {
        Map<String, List<Object>> result = new HashMap<>();
        for (Collection<BoundAndReward> set : acceptanceMDP) {
            int i = 0;
            for (BoundAndReward bound : set) {
                String name = "boundary" + i++;
                List<Object> attributes = new ArrayList<>();
                attributes.add(bound.GOp.limes.toString() + bound.GOp.cmp.toString() + bound.GOp.bound);
                attributes.add("   In this context, the following sets have rewards. ");
                Set<Entry<Integer, TranSet<ProductState<?>>>> entries = bound.relevantEntries();
                for (Entry<Integer, TranSet<ProductState<?>>> entry : entries) {
                    attributes.add("set(" + getTranSetId(entry.getValue()) + ")::" + entry.getKey());
                }
                result.put(name, attributes);
            }
        }
        return result;
    }

    @Override
    public void remove(Collection<Tuple<TranSet<ProductState<?>>, List<TranSet<ProductState<?>>>>> toRemove) {
        toRemove.stream().forEach(pair -> {
            while (acceptanceCondition.indexOf(pair) != -1) {
                int offset = acceptanceCondition.indexOf(pair);
                acceptanceCondition.remove(offset);
                acceptanceMDP.remove(offset);
            }
        });
    }

    @Override
    public boolean implies(Tuple<TranSet<ProductState<?>>, List<TranSet<ProductState<?>>>> premisse, Tuple<TranSet<ProductState<?>>, List<TranSet<ProductState<?>>>> conclusion) {
        int offsetPremisse = acceptanceCondition.indexOf(premisse);
        int offsetConclusion = acceptanceCondition.indexOf(conclusion);
        return super.implies(premisse, conclusion) && acceptanceMDP.get(offsetPremisse).containsAll(acceptanceMDP.get(offsetConclusion));
    }
}
