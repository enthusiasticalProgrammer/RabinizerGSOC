package rabinizer.automata.output;

import java.util.BitSet;
import java.util.Collections;
import java.util.Set;

import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import rabinizer.automata.IState;
import rabinizer.automata.buchi.BuchiAutomaton;
import rabinizer.collections.valuationset.ValuationSetFactory;

public class HOAConsumerBuchi extends HOAConsumerExtended<BuchiAutomaton.State, Set<BuchiAutomaton.State>> {

    public HOAConsumerBuchi(HOAConsumer hoa, ValuationSetFactory valFac, Set<BuchiAutomaton.State> accCond) {
        super(hoa, valFac, accCond);
    }

    public void addEdge(BitSet key, BuchiAutomaton.State end) throws HOAConsumerException {
        addEdgeBackend(valuationSetFactory.createValuationSet(key), end, null);
    }

    @Override
    protected AccType getAccCondition() {
        return AccType.BUCHI;
    }

    @Override
    public void setAcceptanceCondition() throws HOAConsumerException {
        hoa.provideAcceptanceName(AccType.BUCHI.toString(), Collections.emptyList());
        hoa.setAcceptanceCondition(1, mkInf(0));
    }
}
