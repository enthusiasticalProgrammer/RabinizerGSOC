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

package rabinizer.automata.nxt;

import com.google.common.collect.BiMap;
import com.google.common.collect.Sets;
import rabinizer.automata.Master;
import rabinizer.automata.Optimisation;
import rabinizer.collections.Collections3;
import rabinizer.collections.valuationset.BDDValuationSetFactory;
import rabinizer.collections.valuationset.ValuationSet;
import rabinizer.collections.valuationset.ValuationSetFactory;
import rabinizer.ltl.Formula;
import rabinizer.ltl.GOperator;
import rabinizer.ltl.SkeletonVisitor;
import rabinizer.ltl.equivalence.BDDEquivalenceClassFactory;
import rabinizer.ltl.equivalence.EquivalenceClass;
import rabinizer.ltl.equivalence.EquivalenceClassFactory;
import rabinizer.ltl.simplifier.Simplifier;

import java.util.*;

public class DetLimitAutomatonFactory {
    public static DetLimitAutomaton createDetLimitAutomaton(Formula formula) {
        return createDetLimitAutomaton(formula, null, EnumSet.allOf(Optimisation.class));
    }

    public static DetLimitAutomaton createDetLimitAutomaton(Formula formula, BiMap<String, Integer> mapping, Collection<Optimisation> optimisations) {
        ValuationSetFactory valuationSetFactory = new BDDValuationSetFactory(formula, mapping);
        EquivalenceClassFactory equivalenceClassFactory = new BDDEquivalenceClassFactory(formula);

        formula = Simplifier.simplify(formula, Simplifier.Strategy.MODAL_EXT);

        Set<Set<GOperator>> keys = optimisations.contains(Optimisation.SKELETON) ? formula.accept(SkeletonVisitor.getInstance(SkeletonVisitor.SkeletonApproximation.BOTH)) : Sets.powerSet(formula.gSubformulas());

        AcceptingComponent acceptingComponent = new AcceptingComponent(new Master(valuationSetFactory, optimisations), equivalenceClassFactory, valuationSetFactory, optimisations);
        InitialComponent initialComponent = null;

        if (optimisations.contains(Optimisation.IMPATIENT) && Collections3.isSingleton(keys) && ImpatientStateAnalysis.isImpatientFormula(formula)) {
            Set<GOperator> key = Collections3.getElement(keys);

            EquivalenceClass initialClazz = equivalenceClassFactory.createEquivalenceClass(Simplifier.simplify(formula.evaluate(key), Simplifier.Strategy.MODAL_EXT));
            acceptingComponent.jumpInitial(initialClazz, key);
            acceptingComponent.generate();
        } else {
            EquivalenceClass initialClazz = equivalenceClassFactory.createEquivalenceClass(formula);
            initialComponent = new InitialComponent(initialClazz, acceptingComponent, valuationSetFactory, optimisations);
            initialComponent.generate();

            if (optimisations.contains(Optimisation.REMOVE_EPSILON_TRANSITIONS)) {
                removeEpsilonJumps(initialComponent, acceptingComponent, optimisations);
            }
        }

        return new DetLimitAutomaton(initialComponent, acceptingComponent);
    }

    private static void removeEpsilonJumps(InitialComponent initialComponent, AcceptingComponent acceptingComponent, Collection<Optimisation> optimisations) {
        Set<AcceptingComponent.State> accReach = new HashSet<>();

        for (Master.State state : initialComponent.getStates()) {
            Map<Master.State, ValuationSet> successors = initialComponent.getSuccessors(state);
            Map<ValuationSet, List<AcceptingComponent.State>> successorJumps = initialComponent.valuationSetJumps.row(state);

            successors.forEach((successor, vs) -> {
                // Copy successors to a new collection, since clear() will also empty these collections.
                List<AcceptingComponent.State> targets = new ArrayList<>(initialComponent.epsilonJumps.get(successor));
                accReach.addAll(targets);
                successorJumps.put(vs, targets);
            });

            if (optimisations.contains(Optimisation.IMPATIENT)) {
                successors.keySet().removeIf(ImpatientStateAnalysis::isImpatientState);
            }
        }

        initialComponent.epsilonJumps.clear();
        initialComponent.removeUnreachableStates();
        acceptingComponent.removeUnreachableStates(accReach);
    }
}
