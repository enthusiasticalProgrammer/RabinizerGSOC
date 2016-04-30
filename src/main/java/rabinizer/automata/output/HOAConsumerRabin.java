package rabinizer.automata.output;

import java.util.Collection;
import java.util.Collections;

import jhoafparser.ast.AtomAcceptance;
import jhoafparser.ast.BooleanExpression;
import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import rabinizer.automata.DTRA.ProductDegenState;
import rabinizer.automata.RabinPair;
import rabinizer.collections.valuationset.ValuationSetFactory;

public class HOAConsumerRabin extends HOAConsumerAbstractRabin<ProductDegenState, Collection<RabinPair<ProductDegenState>>> {

    public HOAConsumerRabin(HOAConsumer hoa, ValuationSetFactory valFac) {
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
        BooleanExpression<AtomAcceptance> all = new BooleanExpression<>(BooleanExpression.Type.EXP_FALSE, null, null);

        for (RabinPair<ProductDegenState> pair : acc) {
            all = all.or(mkFin(pair.fin).and(mkInf(pair.inf)));
        }

        hoa.setAcceptanceCondition(acceptanceNumbers.size(), all);
    }
}
