package rabinizer.automata;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;

import ltl.UnaryModalOperator;
import omega_automaton.collections.TranSet;
import omega_automaton.collections.Tuple;
import omega_automaton.collections.valuationset.ValuationSet;
import omega_automaton.collections.valuationset.ValuationSetFactory;

public class ProductRabinizer extends Product<RabinSlave.State> {

    protected final Map<UnaryModalOperator, RabinSlave> secondaryAutomata;

    public ProductRabinizer(Master primaryAutomaton, Map<UnaryModalOperator, RabinSlave> slaves, ValuationSetFactory factory, Collection<Optimisation> optimisations) {
        super(primaryAutomaton, factory, optimisations);
        secondaryAutomata = slaves;
    }

    @Override
    protected final State generateInitialState() {
        return new State(primaryAutomaton.getInitialState(), relevantSecondarySlaves(primaryAutomaton.getInitialState()), k -> secondaryAutomata.get(k).getInitialState());
    }

    @Override
    protected Map<UnaryModalOperator, RabinSlave> getSecondaryAutomata() {
        return secondaryAutomata;
    }

    @Override
    protected Set<UnaryModalOperator> getKeys() {
        return secondaryAutomata.keySet();
    }

    protected final TranSet<ProductState> getBuyProductTransitions(RabinSlave slave, Set<MojmirSlave.State> finalStates, int rank) {
        TranSet<ProductState> buyP = new TranSet<>(valuationSetFactory);
        for (ProductState ps : getStates()) {
            State psr = (State) ps;
            RabinSlave.State rs = psr.secondaryStates.get(slave.mojmir.label);
            if (rs != null) { // relevant slave
                buyP.addAll(psr, psr.getBuyTransitions(slave.mojmir, rank, finalStates));
            }
        }

        return buyP;
    }

    Tuple<TranSet<ProductState>, TranSet<ProductState>> createRabinPair(RabinSlave slave, Set<MojmirSlave.State> finalStates, int rank) {
        TranSet<ProductState> failP = getFailingProductTransitions(slave, finalStates);
        TranSet<ProductState> succeedP = getSucceedingProductTransitions(slave, rank, finalStates);
        TranSet<ProductState> buyP = getBuyProductTransitions(slave, finalStates, rank);
        failP.addAll(buyP);
        return new Tuple<>(failP, succeedP);
    }

    public class State extends ProductState {

        protected State(Master.State primaryState, Collection<UnaryModalOperator> keys, Function<UnaryModalOperator, RabinSlave.State> constructor) {
            super(primaryState, keys, constructor);
        }

        protected State(Master.State primaryState, ImmutableMap<UnaryModalOperator, RabinSlave.State> secondaryStates) {
            super(primaryState, secondaryStates);
        }

        private ValuationSet getBuyTransitions(MojmirSlave mojmir, int rank, Set<MojmirSlave.State> finalStates) {
            ValuationSet buy = valuationSetFactory.createEmptyValuationSet();
            RabinSlave.State rs = secondaryStates.get(mojmir.label);
            if (rs != null) { // relevant slave
                buy.addAll(rs.getBuyTrans(rank, finalStates));
            }
            return buy;
        }

        @Override
        protected Map<UnaryModalOperator, RabinSlave> getSecondaryAutomata() {
            return secondaryAutomata;
        }

        @Override
        protected State constructState(Master.State primaryState, ImmutableMap<UnaryModalOperator, RabinSlave.State> secondaryStates) {
            return new State(primaryState, secondaryStates);
        }

        @Override
        public ValuationSet getSucceedTransitions(MojmirSlave mojmir, int rank, Set<MojmirSlave.State> finalStates) {
            ValuationSet succeed = valuationSetFactory.createEmptyValuationSet();
            RabinSlave.State rs = secondaryStates.get(mojmir.label);
            if (rs != null) { // relevant slave
                for (Map.Entry<MojmirSlave.State, Integer> stateIntegerEntry : rs.entrySet()) {
                    if (stateIntegerEntry.getValue() == rank) {
                        succeed.addAll(stateIntegerEntry.getKey().getSucceedMojmirTransitions(finalStates));
                    }
                }
            }

            return succeed;
        }

    }

}
