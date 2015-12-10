package rabinizer.automata;

import rabinizer.ltl.GOperator;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

public class ProductState extends GenericProductState<FormulaAutomatonState, GOperator, RankingState> {

    public ProductState(FormulaAutomatonState primaryState, Map<GOperator, RankingState> secondaryStates) {
        super(primaryState, secondaryStates);
    }

    public ProductState(FormulaAutomatonState primaryState, Collection<GOperator> keys, Function<GOperator, RankingState> constructor) {
        super(primaryState, keys, constructor);
    }
}
