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

import com.google.common.collect.*;
import jhoafparser.consumer.HOAConsumerException;
import rabinizer.automata.IState;
import rabinizer.automata.Master;
import rabinizer.automata.Optimisation;
import rabinizer.automata.SCCAnalyser;
import rabinizer.automata.output.HOAConsumerExtended;
import rabinizer.collections.Collections3;
import rabinizer.collections.Tuple;
import rabinizer.collections.valuationset.ValuationSet;
import rabinizer.collections.valuationset.ValuationSetFactory;
import rabinizer.ltl.Formula;
import rabinizer.ltl.GOperator;
import rabinizer.ltl.SkeletonVisitor;
import rabinizer.ltl.equivalence.EquivalenceClass;

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
        scc = optimisations.contains(Optimisation.SCC);
        impatient = optimisations.contains(Optimisation.IMPATIENT);

        this.acceptingComponent = acceptingComponent;

        epsilonJumps = HashMultimap.create();
        valuationSetJumps = HashBasedTable.create();
    }

    @Override
    public void generate() {
        super.generate();

        SkeletonVisitor visitor = SkeletonVisitor.getInstance(SkeletonVisitor.SkeletonApproximation.BOTH);

        // Generate Jump Table
        List<Set<State>> sccs = scc ? SCCAnalyser.SCCsStates(this, this.getInitialState()) : Collections.singletonList(getStates());

        for (Set<State> scc : sccs) {
            // Skip non-looping states with successors of a singleton SCC.
            if (scc.size() == 1) {
                State state = Collections3.getElement(scc);

                if (!isLooping(state) && hasSuccessors(state)) {
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

    void toHOA(HOAConsumerExtended<IState<?>> consumer) throws HOAConsumerException {
        for (State state : getStates()) {
            consumer.addState(state);

            for (Map.Entry<State, ValuationSet> edge : getSuccessors(state).entrySet()) {
                consumer.addEdge(state, edge.getValue(), edge.getKey());
            }

            for (AcceptingComponent.State accState : epsilonJumps.get(state)) {
                consumer.addEpsilonEdge(state, accState);
            }

            for (Map.Entry<ValuationSet, List<AcceptingComponent.State>> entry : valuationSetJumps.row(state).entrySet()) {
                for (AcceptingComponent.State accState : entry.getValue()) {
                    consumer.addEdge(state, entry.getKey(), accState);
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
