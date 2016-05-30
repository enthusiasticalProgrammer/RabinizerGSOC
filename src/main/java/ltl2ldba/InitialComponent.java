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

package ltl2ldba;

import com.google.common.collect.*;
import jhoafparser.consumer.HOAConsumerException;
import rabinizer.automata.*;
import rabinizer.automata.output.HOAConsumerExtended;
import ltl.Collections3;
import rabinizer.collections.valuationset.ValuationSet;
import rabinizer.collections.valuationset.ValuationSetFactory;
import ltl.Formula;
import ltl.GOperator;
import ltl.SkeletonVisitor;
import ltl.equivalence.EquivalenceClass;

import java.util.*;

public class InitialComponent extends Master {
    public final SetMultimap<State, AcceptingComponent.State> epsilonJumps;
    public final Table<State, ValuationSet, List<AcceptingComponent.State>> valuationSetJumps;

    private final boolean skeleton;
    private final boolean scc;
    private final boolean impatient;
    private final AcceptingComponent acceptingComponent;

    InitialComponent(EquivalenceClass initialClazz, AcceptingComponent acceptingComponent, ValuationSetFactory valuationSetFactory, Collection<Optimisation> optimisations) {
        super(initialClazz, valuationSetFactory, optimisations);

        skeleton = optimisations.contains(Optimisation.SKELETON);
        scc = optimisations.contains(Optimisation.SCC_ANALYSIS);
        impatient = optimisations.contains(Optimisation.IMPATIENT);

        this.acceptingComponent = acceptingComponent;

        epsilonJumps = HashMultimap.create();
        valuationSetJumps = HashBasedTable.create();
    }

    public InitialComponent(EquivalenceClass init, AcceptingComponent acceptingComponent, ValuationSetFactory valuationSetFactory) {
        super(init, valuationSetFactory, Collections.emptySet());
        initialState = new Master.State(init);
        transitions.put(getInitialState(), Collections.emptyMap());
        epsilonJumps = HashMultimap.create();
        epsilonJumps.put(getInitialState(), acceptingComponent.getInitialState());
        valuationSetJumps = HashBasedTable.create();
        skeleton = false;
        scc = false;
        impatient = false;
        this.acceptingComponent = acceptingComponent;
    }

    @Override
    public void generate() {
        super.generate();

        SkeletonVisitor visitor = SkeletonVisitor.getInstance(SkeletonVisitor.SkeletonApproximation.BOTH);

        // Generate Jump Table
        List<Set<State>> sccs = scc ? SCCAnalyser.SCCsStates(this) : Collections.singletonList(getStates());

        for (Set<State> scc : sccs) {
            // Skip non-looping states with successors of a singleton SCC.
            if (scc.size() == 1) {
                State state = Collections3.getElement(scc);

                if (isTransient(state) && hasSuccessors(state)) {
                    continue;
                }
            }

            for (State state : scc) {
                Formula stateFormula = state.getClazz().getRepresentative();
                Set<Set<GOperator>> keys = skeleton ? stateFormula.accept(visitor) : Sets.powerSet(stateFormula.gSubformulas());

                for (Set<GOperator> key : keys) {
                    AcceptingComponent.State successor = acceptingComponent.jump(state.getClazz(), key);

                    if (successor == null) {
                        continue;
                    }

                    epsilonJumps.put(state, successor);
                }
            }
        }
    }

    void toHOA(HOAConsumerGeneralisedBuchi<IState<?>> consumer) throws HOAConsumerException {
        for (State state : getStates()) {
            consumer.addState(state);

            for (Map.Entry<State, ValuationSet> edge : getSuccessors(state).entrySet()) {
                consumer.addEdge(edge.getValue(), edge.getKey());
            }

            for (AcceptingComponent.State accState : epsilonJumps.get(state)) {
                consumer.addEpsilonEdge(accState);
            }

            for (Map.Entry<ValuationSet, List<AcceptingComponent.State>> entry : valuationSetJumps.row(state).entrySet()) {
                for (AcceptingComponent.State accState : entry.getValue()) {
                    consumer.addEdge(entry.getKey(), accState);
                }
            }

            consumer.stateDone();
        }
    }

    @Override
    protected boolean suppressEdge(EquivalenceClass current, EquivalenceClass successor) {
        return super.suppressEdge(current, successor) || (impatient && ImpatientStateAnalysis.isImpatientClazz(current));
    }
}
