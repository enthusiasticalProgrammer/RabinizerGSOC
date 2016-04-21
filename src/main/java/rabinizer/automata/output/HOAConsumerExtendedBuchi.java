package rabinizer.automata.output;

import java.util.BitSet;
import java.util.Collections;
import java.util.Set;

import jhoafparser.ast.BooleanExpression;
import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import rabinizer.automata.buchi.BuchiAutomaton;
import rabinizer.collections.valuationset.ValuationSetFactory;

public class HOAConsumerExtendedBuchi extends HOAConsumerExtended<BuchiAutomaton.State, Set<BuchiAutomaton.State>> {

    public HOAConsumerExtendedBuchi(HOAConsumer hoa, ValuationSetFactory valFac) {
        super(hoa, valFac);
    }

    @Override
    protected AccType getAccCondition(Set<BuchiAutomaton.State> acc) {
        return AccType.BUCHI;
    }

    @Override
    protected void setAccCondForHOAConsumer(Set<BuchiAutomaton.State> infConds) throws HOAConsumerException {
        hoa.provideAcceptanceName(AccType.BUCHI.toString(), Collections.emptyList());
        hoa.setAcceptanceCondition(1, new BooleanExpression<>(mkInf(0)));
    }

    public void addEdge(BuchiAutomaton.State begin, BitSet key, BuchiAutomaton.State end) throws HOAConsumerException {
        addEdgeBackend(begin, valuationSetFactory.createValuationSet(key).toFormula(), end, null);
    }
}
