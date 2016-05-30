package rabinizer.automata.output;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import jhoafparser.ast.AtomAcceptance;
import jhoafparser.ast.BooleanExpression;
import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import rabinizer.automata.GeneralizedRabinPair;
import rabinizer.automata.Product;
import rabinizer.automata.TranSet;
import ltl.Collections3;
import rabinizer.collections.valuationset.ValuationSetFactory;

public class HOAConsumerGeneralisedRabin extends HOAConsumerAbstractRabin<Product.ProductState, List<GeneralizedRabinPair<Product.ProductState>>> {

    public HOAConsumerGeneralisedRabin(HOAConsumer hoa, ValuationSetFactory valFac, Product.ProductState initialState, List<GeneralizedRabinPair<Product.ProductState>> accCond) {
        super(hoa, valFac, initialState, accCond);
    }

    @Override
    protected AccType getAccCondition() {
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
    public void setAcceptanceCondition() {
        try {
            AccType accT = getAccCondition();
            hoa.provideAcceptanceName(accT.toString(), getAccList());

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
        } catch (HOAConsumerException ex) {
            // We wrap HOAConsumerException into an unchecked exception in order to keep the interfaces clean and tidy.
            throw new RuntimeException(ex);
        }
    }

    private List<Object> getAccList() {
        List<Object> result=new ArrayList<Object>();
        result.add(acc.size());
        for (GeneralizedRabinPair<Product.ProductState> singleCond : acc) {
            result.add(singleCond.infs.size());
        }
        return result;
    }
}
