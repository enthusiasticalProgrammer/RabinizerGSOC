package rabinizer.automata.output;

import java.util.BitSet;
import java.util.Collections;
import java.util.Set;

import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import rabinizer.automata.buchi.BuchiAutomaton;
import rabinizer.automata.buchi.BuchiAutomaton.State;
import rabinizer.collections.valuationset.ValuationSetFactory;

public class HOAConsumerBuchi extends HOAConsumerExtended<BuchiAutomaton.State, Set<BuchiAutomaton.State>> {

    public HOAConsumerBuchi(HOAConsumer hoa, ValuationSetFactory valFac) {
        super(hoa, valFac);
    }

    public void addEdge(BitSet key, BuchiAutomaton.State end) throws HOAConsumerException {
        addEdgeBackend(valuationSetFactory.createValuationSet(key), end, null);
    }

    @Override
    public void setAcceptanceCondition(Set<State> acc) throws HOAConsumerException {
        hoa.provideAcceptanceName(AccType.BUCHI.toString(), Collections.emptyList());
        hoa.setAcceptanceCondition(1, mkInf(0));
    }
}
