package rabinizer.automata;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import ltl.FrequencyG;
import ltl.GOperator;
import ltl.ModalOperator;
import omega_automaton.collections.TranSet;
import omega_automaton.collections.valuationset.ValuationSet;
import omega_automaton.collections.valuationset.ValuationSetFactory;
import rabinizer.automata.MojmirSlave.State;
import rabinizer.frequencyLTL.FOperatorForMojmir;

public class ProductControllerSynthesis extends Product {

    protected final Map<ModalOperator, FrequencySelfProductSlave> secondaryAutomata;

    public ProductControllerSynthesis(Master primaryAutomaton, Map<ModalOperator, FrequencySelfProductSlave> slaves, ValuationSetFactory factory,
            Collection<Optimisation> optimisations) {
        super(primaryAutomaton, factory, optimisations);
        this.secondaryAutomata = slaves;
    }

    @Override
    protected final State generateInitialState() {
        return new State(primaryAutomaton.getInitialState(), relevantSecondarySlaves(primaryAutomaton.getInitialState()), k -> secondaryAutomata.get(k).getInitialState());
    }

    @Override
    protected Map<ModalOperator, FrequencySelfProductSlave> getSecondaryAutomata() {
        return secondaryAutomata;
    }

    @Override
    protected Set<ModalOperator> getKeys() {
        return secondaryAutomata.keySet();
    }

    public TranSet<ProductState<?>> getControllerAcceptanceF(FOperatorForMojmir f, Set<MojmirSlave.State> finalStates) {
        return getSucceedingProductTransitions(secondaryAutomata.get(f), -1, finalStates);
    }

    public TranSet<ProductState<?>> getControllerAcceptanceG(GOperator g, Set<MojmirSlave.State> finalStates) {
        return getFailingProductTransitions(secondaryAutomata.get(g), finalStates);
    }

    public Map<TranSet<ProductState<?>>, Integer> getControllerAcceptanceFrequencyG(FrequencyG g, Set<MojmirSlave.State> finalStates) {
        Map<TranSet<ProductState<?>>, Integer> result = new HashMap<>();
        int maxTokenNumber = secondaryAutomata.get(g).mojmir.size();
        for (int i = 0; i <= maxTokenNumber; i++) {
            TranSet<ProductState<?>> relevantTransitions = getSucceedingProductTransitions(secondaryAutomata.get(g), i, finalStates);
            if (!relevantTransitions.isEmpty()) {
                result.put(relevantTransitions, i);
            }
        }
        return result;
    }

    public class State extends ProductState<FrequencySelfProductSlave.State> {

        protected State(Master.State primaryState, Collection<ModalOperator> keys, Function<ModalOperator, FrequencySelfProductSlave.State> constructor) {
            super(primaryState, keys, constructor);
        }

        protected State(Master.State primaryState, ImmutableMap<ModalOperator, FrequencySelfProductSlave.State> secondaryStates) {
            super(primaryState, secondaryStates);
        }

        @Override
        protected Map<ModalOperator, FrequencySelfProductSlave> getSecondaryAutomata() {
            return secondaryAutomata;
        }

        @Override
        protected State constructState(Master.State primaryState, ImmutableMap<ModalOperator, FrequencySelfProductSlave.State> secondaryStates) {
            return new State(primaryState, secondaryStates);
        }

        @Override
        public ValuationSet getSucceedTransitions(MojmirSlave mojmir, int rank, Set<MojmirSlave.State> finalStates) {
            ValuationSet succeed = valuationSetFactory.createEmptyValuationSet();
            FrequencySelfProductSlave.State rs = secondaryStates.get(mojmir.label);
            if (rs != null) { // relevant slave
                if (rank == -1) {
                    for (Map.Entry<MojmirSlave.State, Integer> stateIntegerEntry : rs.entrySet()) {
                        succeed.addAll(stateIntegerEntry.getKey().getSucceedMojmirTransitions(finalStates));
                    }
                    return succeed;
                } else {
                    Map<MojmirSlave.State, ValuationSet> succeedingTransitions = new HashMap<>();
                    for (MojmirSlave.State state : rs.keySet()) {
                        ValuationSet succeedingForState = state.getSucceedMojmirTransitions(finalStates);
                        if (!succeedingForState.isEmpty()) {
                            succeedingTransitions.put(state, state.getSucceedMojmirTransitions(finalStates));
                        }
                    }

                    for (Set<MojmirSlave.State> stateSet : Sets.powerSet(succeedingTransitions.keySet())) {
                        if (stateSet.stream().mapToInt(s -> rs.get(s)).sum() == rank) {
                            ValuationSet succeeder = valuationSetFactory.createUniverseValuationSet();
                            for (MojmirSlave.State state : stateSet) {
                                succeeder.retainAll(succeedingTransitions.get(state));
                            }
                            succeed.addAll(succeeder);
                        }
                    }
                }
            }

            return succeed;
        }

    }
}
