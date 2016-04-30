package rabinizer.automata.output;

import java.util.Collection;
import java.util.Collections;

import jhoafparser.ast.AtomAcceptance;
import jhoafparser.ast.BooleanExpression;
import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import rabinizer.automata.GeneralizedRabinPair;
import rabinizer.automata.Product;
import rabinizer.automata.TranSet;
import rabinizer.collections.Collections3;
import rabinizer.collections.valuationset.ValuationSetFactory;

public class HOAConsumerGeneralisedRabin extends HOAConsumerAbstractRabin<Product.ProductState, Collection<GeneralizedRabinPair<Product.ProductState>>> {

    public HOAConsumerGeneralisedRabin(HOAConsumer hoa, ValuationSetFactory valFac) {
        super(hoa, valFac);
    }

    @Override
    protected AccType getAccCondition(Collection<GeneralizedRabinPair<Product.ProductState>> acc) {
        if (acc.isEmpty()) {
            return AccType.NONE;
        }

        if (acc.size() == 1) {
            GeneralizedRabinPair<Product.ProductState> pair = Collections3.getElement(acc);

            if (pair.fin.isEmpty() || pair.infs.size() == 1) {
                return AccType.BUCHI;
            }

            if (pair.infs.isEmpty()) {
                return AccType.COBUCHI;
            }
        }

        if (acc.stream().allMatch(pair -> pair.fin.isEmpty())) {
            return AccType.GENBUCHI;
        }

        if (acc.stream().allMatch(pair -> pair.infs.size() <= 1)) {
            return AccType.RABIN;
        }

        return AccType.GENRABIN;
    }

    @Override
    protected void setAccCondForHOAConsumer(Collection<GeneralizedRabinPair<Product.ProductState>> acc) throws HOAConsumerException {
        BooleanExpression<AtomAcceptance> all = new BooleanExpression<>(BooleanExpression.Type.EXP_FALSE, null, null);

        for (GeneralizedRabinPair<Product.ProductState> genRabinPair : acc) {
            BooleanExpression<AtomAcceptance> both = new BooleanExpression<>(BooleanExpression.Type.EXP_TRUE, null, null);

            if (!genRabinPair.fin.isEmpty()) {
                both = mkFin(genRabinPair.fin);
            }

            for (TranSet<Product.ProductState> inf : genRabinPair.infs) {
                both = both.and(mkInf(inf));
            }

            all = all.or(both);
        }

        hoa.setAcceptanceCondition(acceptanceNumbers.size(), new RemoveConstants<AtomAcceptance>().visit(all));
    }

    @Override
    public void setAcceptanceCondition(Collection<GeneralizedRabinPair<Product.ProductState>> acc) throws HOAConsumerException {
        AccType accT = getAccCondition(acc);
        hoa.provideAcceptanceName(accT.toString(), Collections.emptyList());
        setAccCondForHOAConsumer(acc);
    }
}
