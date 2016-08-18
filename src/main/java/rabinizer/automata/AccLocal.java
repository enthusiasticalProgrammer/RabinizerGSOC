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

import com.google.common.collect.Sets;
import ltl.Conjunction;
import ltl.Formula;
import ltl.GOperator;
import ltl.UnaryModalOperator;
import ltl.equivalence.EquivalenceClass;
import ltl.equivalence.EquivalenceClassFactory;
import omega_automaton.collections.Collections3;
import omega_automaton.collections.TranSet;
import omega_automaton.collections.valuationset.ValuationSetFactory;
import rabinizer.automata.MojmirSlave.State;
import rabinizer.frequencyLTL.FOperatorForMojmir;
import rabinizer.frequencyLTL.SkeletonVisitor;
import rabinizer.frequencyLTL.SlaveSubformulaVisitor;
import rabinizer.frequencyLTL.TopMostOperatorVisitor;

import java.util.*;

/**
 * @param <AccMasterInput>
 *            AccMaster is always a Map
 *            <AccMasterInput,AccMasterOutput> Therefore it has to be separated
 *            into the two types AccMasterInput and AccMasterOutput
 *
 * @param <AccSlaves>
 *            This type of parameter indicates, which type a single
 *            slave-acceptance may have.
 * @param <P>
 *            The type of the Product (either ProductRabinizer or
 *            ProductControllerSynthesis)
 */
abstract class AccLocal<AccMasterInput, AccMasterOutput, AccSlaves, ParamProduct extends AbstractSelfProductSlave<ParamProduct>.State, P extends Product<ParamProduct>> {

    protected final ValuationSetFactory valuationSetFactory;
    protected final EquivalenceClassFactory equivalenceClassFactory;
    protected final Map<UnaryModalOperator, Set<UnaryModalOperator>> topmostSlaves = new HashMap<>();
    protected final Collection<Optimisation> optimisations;
    protected final P product;

    public AccLocal(P product, ValuationSetFactory valuationSetFactory, EquivalenceClassFactory equivalenceFactory, Collection<Optimisation> opts) {
        this.product = product;
        this.valuationSetFactory = valuationSetFactory;
        this.equivalenceClassFactory = equivalenceFactory;
        this.optimisations = opts;

        for (UnaryModalOperator gOperator : getOverallFormula().accept(new SlaveSubformulaVisitor())) {
            topmostSlaves.put(gOperator, (gOperator.operand).accept(new TopMostOperatorVisitor()));
        }
    }

    public final Map<AccMasterInput, AccMasterOutput> computeAccMasterOptions() {
        Map<AccMasterInput, AccMasterOutput> result = new HashMap<>();

        Set<Set<UnaryModalOperator>> gSets;

        if (optimisations.contains(Optimisation.SKELETON)) {
            gSets = getOverallFormula().accept(new SkeletonVisitor());
        } else {
            gSets = Sets.powerSet(getOverallFormula().accept(new SlaveSubformulaVisitor()));
        }

        for (Set<UnaryModalOperator> gSet : gSets) {
            computeAccMasterForASingleGSet(gSet, result);
        }
        return result;
    }

    /**
     * This method fills up the AccMaster-map for a certain Set of Slave
     * operators which are to be true.
     */
    protected abstract void computeAccMasterForASingleGSet(Set<UnaryModalOperator> gSet, Map<AccMasterInput, AccMasterOutput> result);

    public final Map<UnaryModalOperator, Map<Set<UnaryModalOperator>, AccSlaves>> getAllSlaveAcceptanceConditions() {
        Map<UnaryModalOperator, Map<Set<UnaryModalOperator>, AccSlaves>> result = new HashMap<>();
        for (UnaryModalOperator g : product.getSecondaryAutomata().keySet()) {
            result.put(g, computeAccSlavesOptions(g));
        }

        return result;
    }

    private final Map<Set<UnaryModalOperator>, AccSlaves> computeAccSlavesOptions(UnaryModalOperator g) {
        Map<Set<UnaryModalOperator>, AccSlaves> result = new HashMap<>();

        Set<Set<UnaryModalOperator>> gSets = Sets.powerSet(topmostSlaves.get(g));

        for (Set<UnaryModalOperator> gSet : gSets) {
            Set<MojmirSlave.State> finalStates = new HashSet<>();
            EquivalenceClass gSetClazz = equivalenceClassFactory.createEquivalenceClass(new Conjunction(gSet));

            for (MojmirSlave.State fs : product.getSecondaryAutomata().get(g).mojmir.getStates()) {
                if (gSetClazz.implies(fs.getClazz())) {
                    finalStates.add(fs);
                }
            }
            result.put(gSet, getSingleSlaveAccCond(g, finalStates));
        }

        return result;
    }

    protected abstract AccSlaves getSingleSlaveAccCond(UnaryModalOperator g, Set<State> finalStates);

    protected final Formula getOverallFormula() {
        return product.primaryAutomaton.getInitialState().getClazz().getRepresentative();
    }

    /**
     * A wrapper, which defines a ranking, which acts for
     * AccLocalControllerSynthesis as if it was not there
     */
    protected final TranSet<Product<ParamProduct>.ProductState> computeNonAccMasterTransForStateIgoringRankings(Set<UnaryModalOperator> gSet,
            Product<ParamProduct>.ProductState ps) {
        Map<UnaryModalOperator, Integer> ranking = new HashMap<>();
        gSet.forEach(g -> ranking.put(g, -1));
        return computeNonAccMasterTransForState(ranking, ps);
    }

    protected final TranSet<Product<ParamProduct>.ProductState> computeNonAccMasterTransForState(Map<UnaryModalOperator, Integer> ranking, Product<ParamProduct>.ProductState ps) {
        TranSet<Product<ParamProduct>.ProductState> result = new TranSet<>(valuationSetFactory);

        if (optimisations.contains(Optimisation.EAGER)) {
            BitSet sensitiveAlphabet = ps.getSensitiveAlphabet();

            for (BitSet valuation : Collections3.powerSet(sensitiveAlphabet)) {
                if (!slavesEntail(ps, ranking, valuation, ps.primaryState.getClazz())) {
                    result.addAll(ps, valuationSetFactory.createValuationSet(valuation, sensitiveAlphabet));
                }
            }
        } else {
            if (!slavesEntail(ps, ranking, null, ps.primaryState.getClazz())) {
                result.addAll(ps, valuationSetFactory.createUniverseValuationSet());
            }
        }

        return result;
    }

    private final boolean slavesEntail(Product<ParamProduct>.ProductState ps, Map<UnaryModalOperator, Integer> ranking, BitSet valuation, EquivalenceClass consequent) {
        Collection<Formula> conjunction = new ArrayList<>(3 * ranking.size());

        for (Map.Entry<UnaryModalOperator, Integer> entry : ranking.entrySet()) {
            UnaryModalOperator G = entry.getKey();
            int rank = entry.getValue();

            conjunction.add(G);
            if (optimisations.contains(Optimisation.EAGER)) {
                conjunction.add(G.operand);
            }

            AbstractSelfProductSlave<? extends AbstractSelfProductSlave<?>.State>.State rs = ps.getSecondaryState(G);
            if (rs != null) {
                for (Map.Entry<MojmirSlave.State, Integer> stateEntry : rs.entrySet()) {
                    if (stateEntry.getValue() >= rank) {
                        if (optimisations.contains(Optimisation.EAGER)) {
                            conjunction.add(stateEntry.getKey().getClazz().getRepresentative().temporalStep(valuation));
                        }
                        conjunction.add(stateEntry.getKey().getClazz().getRepresentative());
                    }
                }
            }
        }

        if (optimisations.contains(Optimisation.EAGER)) {
            consequent = consequent.temporalStep(valuation);
        }

        EquivalenceClass antecedent = equivalenceClassFactory.createEquivalenceClass(new Conjunction(conjunction), formula -> {
            if ((formula instanceof GOperator || formula instanceof FOperatorForMojmir) && !ranking.containsKey(formula)) {
                return Optional.of(Boolean.FALSE);
            }

            return Optional.empty();
        });

        return antecedent.implies(consequent);
    }
}