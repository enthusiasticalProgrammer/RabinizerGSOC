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
import rabinizer.ltl.Formula;

public class HOAConsumerExtendedGeneralisedBuchi<T> extends HOAConsumerExtended<T, Integer> {

    public HOAConsumerExtendedGeneralisedBuchi(HOAConsumer hoa, ValuationSetFactory valFac) {
        super(hoa, valFac);
    }

    @Override
    protected AccType getAccCondition(Integer acc) {
        return AccType.GENBUCHI;
    }

    @Override
    protected void setAccCondForHOAConsumer(Integer acc) throws HOAConsumerException {
        hoa.provideAcceptanceName(AccType.GENBUCHI.toString(), Collections.singletonList(acc));
        hoa.setAcceptanceCondition(acc, mkInfAnd(acc));
    }

    private static BooleanExpression<AtomAcceptance> mkInfAnd(int j) {
        BooleanExpression<AtomAcceptance> conjunction = new BooleanExpression<>(mkInf(0));

        for (int i = 1; i < j; i++) {
            conjunction = conjunction.and(new BooleanExpression<>(mkInf(i)));
        }

        return conjunction;
    }

    public void addEdge(T state, ValuationSet value, T key) throws HOAConsumerException {
        addEdgeBackend(state, value.toFormula(), key, null);

    }

    public void addEdge(T begin, Formula label, T successor, BitSet key) throws HOAConsumerException {
        addEdgeBackend(begin, label, successor, Collections3.toList(key));
    }

    public void addEpsilonEdge(T begin, T successor) throws HOAConsumerException {
        Main.nonsilent("Warning: HOA does not support epsilon-transitions. (" + begin + " -> " + successor + ")");
        hoa.addEdgeWithLabel(getStateId(begin), null, Collections.singletonList(getStateId(successor)), null);
    }
}
