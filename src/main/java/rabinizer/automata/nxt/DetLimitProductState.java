package rabinizer.automata.nxt;

import rabinizer.automata.FormulaAutomatonState;
import rabinizer.automata.GenericProductState;
import rabinizer.ltl.GOperator;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

public class DetLimitProductState extends GenericProductState<FormulaAutomatonState, GOperator, DetLimitSlaveState> {
    public DetLimitProductState(FormulaAutomatonState primaryState, Map<GOperator, DetLimitSlaveState> secondaryStates) {
        super(primaryState, secondaryStates);
    }

    public DetLimitProductState(FormulaAutomatonState primaryState, Collection<GOperator> keys, Function<GOperator, DetLimitSlaveState> constructor) {
        super(primaryState, keys, constructor);
    }
}
