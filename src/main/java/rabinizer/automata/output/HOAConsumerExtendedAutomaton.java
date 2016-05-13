package rabinizer.automata.output;

import java.util.Collections;

import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import rabinizer.collections.valuationset.ValuationSet;
import rabinizer.collections.valuationset.ValuationSetFactory;

public class HOAConsumerExtendedAutomaton<S, C> extends HOAConsumerExtended<S, C> {

    public HOAConsumerExtendedAutomaton(HOAConsumer hoa, ValuationSetFactory valSetFac) {
        super(hoa, valSetFac);
    }

    @Override
    protected AccType getAccCondition(C acc) {
        return AccType.ALL;
    }

    @Override
    public void setAcceptanceCondition(C acc) throws HOAConsumerException {
        hoa.provideAcceptanceName(getAccCondition(acc).toString(), Collections.emptyList());
    }

    public void addEdge(ValuationSet key, S end) throws HOAConsumerException {
        addEdgeBackend(key, end, null);
    }

}
