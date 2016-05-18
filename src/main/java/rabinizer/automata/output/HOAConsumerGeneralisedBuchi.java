package rabinizer.automata.output;

import java.util.BitSet;
import java.util.Collections;

import jhoafparser.ast.AtomAcceptance;
import jhoafparser.ast.BooleanExpression;
import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import rabinizer.collections.Collections3;
import rabinizer.collections.valuationset.ValuationSet;
import rabinizer.collections.valuationset.ValuationSetFactory;
import rabinizer.exec.Main;

public class HOAConsumerGeneralisedBuchi<T> extends HOAConsumerExtended<T, Integer> {

    public HOAConsumerGeneralisedBuchi(HOAConsumer hoa, ValuationSetFactory valFac, Integer accCond) {
        super(hoa, valFac, accCond);
    }

    @Override
    public void addEdge(ValuationSet label, T key) {
        addEdgeBackend(label, key, null);
    }

    public void addEdge(ValuationSet label, T successor, BitSet acceptingLevels) {
        addEdgeBackend(label, successor, Collections3.toList(acceptingLevels));
    }

    public void addEpsilonEdge(T successor) {
        try {
            Main.nonsilent("Warning: HOA currently does not support epsilon-transitions. (" + currentState + " -> " + successor + ')');
            hoa.addEdgeWithLabel(getStateId(currentState), null, Collections.singletonList(getStateId(successor)), null);
        } catch (HOAConsumerException ex) {
            // We wrap HOAConsumerException into an unchecked exception in order to keep the interfaces clean and tidy.
            throw new RuntimeException(ex);
        }
    }

    @Override
    protected AccType getAccCondition() {
        return AccType.GENBUCHI;
    }

    @Override
    public void setAcceptanceCondition() throws HOAConsumerException {
        hoa.provideAcceptanceName(getAccCondition().toString(), Collections.singletonList(acc));
        BooleanExpression<AtomAcceptance> conjunction = mkInf(0);

        for (int i = 1; i < acc; i++) {
            conjunction = conjunction.and(mkInf(i));
        }

        hoa.setAcceptanceCondition(acc, conjunction);
    }
}
