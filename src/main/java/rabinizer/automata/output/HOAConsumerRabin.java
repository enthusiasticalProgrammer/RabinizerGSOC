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

    public HOAConsumerRabin(HOAConsumer hoa, ValuationSetFactory valFac, ProductDegenState initialState, Collection<RabinPair<ProductDegenState>> accCond) {
        super(hoa, valFac, initialState, accCond);
    }

    @Override
    protected AccType getAccCondition() {
        if (acc.isEmpty()) {
            return AccType.NONE;
        }

        return AccType.RABIN;
    }

    @Override
    public void setAcceptanceCondition() {
        try {
            // TODO: This violates the HOAF specification
            hoa.provideAcceptanceName("Rabin", Collections.nCopies(1, acc.size()));
            BooleanExpression<AtomAcceptance> all = new BooleanExpression<>(BooleanExpression.Type.EXP_FALSE, null, null);

            for (RabinPair<ProductDegenState> pair : acc) {
                all = all.or(mkFin(pair.fin).and(mkInf(pair.inf)));
            }

            hoa.setAcceptanceCondition(acceptanceNumbers.size(), all);
        } catch (HOAConsumerException ex) {
            // We wrap HOAConsumerException into an unchecked exception in order to keep the interfaces clean and tidy.
            throw new RuntimeException(ex);
        }
    }
}
