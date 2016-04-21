package rabinizer.automata.output;

import java.util.Collection;
import java.util.Collections;

import jhoafparser.ast.BooleanExpression;
import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import rabinizer.automata.DTRA.ProductDegenState;
import rabinizer.automata.RabinPair;
import rabinizer.collections.valuationset.ValuationSetFactory;

public class HOAConsumerExtendedRabin extends HOAConsumerExtendedAbstractRabin<ProductDegenState, Collection<RabinPair<ProductDegenState>>> {

    public HOAConsumerExtendedRabin(HOAConsumer hoa, ValuationSetFactory valFac) {
        super(hoa, valFac);
    }

    @Override
    protected AccType getAccCondition(Collection<RabinPair<ProductDegenState>> acc) {
        if (acc.isEmpty()) {
            return AccType.NONE;
        }
        return AccType.RABIN;
    }

    @Override
    protected void setAccCondForHOAConsumer(Collection<RabinPair<ProductDegenState>> acc) throws HOAConsumerException {
        hoa.provideAcceptanceName("Rabin", Collections.nCopies(1, acc.size()));

        BooleanExpression all = new BooleanExpression<>(BooleanExpression.Type.EXP_FALSE, null, null);
        for (RabinPair<ProductDegenState> pair : acc) {
            BooleanExpression both = new BooleanExpression<>(mkFin(pair.fin));
            both = both.and(new BooleanExpression<>(mkInf(pair.inf)));
            all = all.or(both);
        }
        hoa.setAcceptanceCondition(acceptanceNumbers.size(), all);
    }
}
