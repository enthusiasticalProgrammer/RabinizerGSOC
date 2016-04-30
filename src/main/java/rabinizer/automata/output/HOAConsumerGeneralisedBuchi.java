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

    public HOAConsumerGeneralisedBuchi(HOAConsumer hoa, ValuationSetFactory valFac) {
        super(hoa, valFac);
    }

    public void setAcceptanceCondition(Integer acc) throws HOAConsumerException {
        hoa.provideAcceptanceName(AccType.GENBUCHI.toString(), Collections.singletonList(acc));
        BooleanExpression<AtomAcceptance> conjunction = mkInf(0);

        for (int i = 1; i < acc; i++) {
            conjunction = conjunction.and(mkInf(i));
        }

        hoa.setAcceptanceCondition(acc, conjunction);
    }

    public void addEdge(ValuationSet label, T key) throws HOAConsumerException {
        addEdgeBackend(label, key, null);
    }

    public void addEdge(ValuationSet label, T successor, BitSet acceptingLevels) throws HOAConsumerException {
        addEdgeBackend(label, successor, Collections3.toList(acceptingLevels));
    }

    public void addEpsilonEdge(T successor) throws HOAConsumerException {
        Main.nonsilent("Warning: HOA does not support epsilon-transitions. (" + currentState + " -> " + successor + ')');
        hoa.addEdgeWithLabel(getStateId(currentState), null, Collections.singletonList(getStateId(successor)), null);
    }
}
