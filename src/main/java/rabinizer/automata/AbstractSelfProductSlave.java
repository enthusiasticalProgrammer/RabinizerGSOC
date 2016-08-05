package rabinizer.automata;

import java.util.HashMap;
import java.util.Map;

import omega_automaton.Automaton;
import omega_automaton.AutomatonState;
import omega_automaton.acceptance.AllAcceptance;
import omega_automaton.collections.valuationset.ValuationSetFactory;

public abstract class AbstractSelfProductSlave<S extends AutomatonState<S>> extends Automaton<S, AllAcceptance> {
    protected final MojmirSlave mojmir;

    protected AbstractSelfProductSlave(MojmirSlave mojmir, ValuationSetFactory factory) {
        super(factory);
        this.mojmir = mojmir;
    }

    @Override
    protected S generateInitialState() {
        Map<MojmirSlave.State, Integer> map = new HashMap<>();
        map.put(mojmir.getInitialState(), 1);
        return generateState(map);
    }

    /**
     * Actually both implementations do the same, but due to the S-parameter it
     * has to be abstract
     */
    protected abstract S generateState(Map<MojmirSlave.State, Integer> map);

    /**
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!WARNING!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     * In one subclass the Integer corresponds to the rank according to the
     * paper describing Rabinizer 3.1 and in the other subclass it corresponds
     * to the amount of tokens being in the state currently
     */
    public abstract class State extends HashMap<MojmirSlave.State, Integer> implements AutomatonState<S> {
        private static final long serialVersionUID = 1L;

        @Override
        public String toString() {
            String result = "";
            for (MojmirSlave.State f : keySet()) {
                result += " " + f + "=" + get(f);
            }
            return result;
        }

        /*
         * @Override public BitSet getSensitiveAlphabet() { BitSet alphabet =
         * new BitSet(); this.forEach((state, rank) ->
         * alphabet.or(state.getSensitiveAlphabet())); return alphabet; }
         */

        @Override
        public ValuationSetFactory getFactory() {
            return valuationSetFactory;
        }
    }
}
