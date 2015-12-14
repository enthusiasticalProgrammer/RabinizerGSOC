package rabinizer.automata.nxt;

import rabinizer.automata.Automaton;
import rabinizer.ltl.*;

import java.util.Objects;
import java.util.Set;

public class DetLimitSlave extends Automaton<DetLimitSlaveState> {

    private final EquivalenceClass initialFormula;
    private final EquivalenceClass TRUE;
    private final boolean impCover;

    public DetLimitSlave(Formula formula, boolean impCover, EquivalenceClassFactory equivalenceClassFactory, ValuationSetFactory<String> valuationSetFactory) {
        super(valuationSetFactory);
        this.initialFormula = equivalenceClassFactory.createEquivalenceClass(formula);
        this.TRUE = equivalenceClassFactory.getTrue();
        this.impCover = impCover;
    }

    public static boolean isAcceptingTransition(DetLimitSlaveState s, ValuationSet vs) {
        EquivalenceClass succ = s.current.unfold(true).temporalStep(vs.pickAny());
        return succ.isTrue();
    }

    @Override
    protected DetLimitSlaveState generateInitialState() {
        return new DetLimitSlaveState(initialFormula, TRUE);
    }

    @Override
    protected DetLimitSlaveState generateSuccState(DetLimitSlaveState s, ValuationSet vs) {
        EquivalenceClass succ = s.current.unfold(true).temporalStep(vs.pickAny());

        if (succ.isTrue()) {
            // Since we are done with succ, we can swap states.
            EquivalenceClass current = s.next.unfold(true).temporalStep(vs.pickAny());
            return new DetLimitSlaveState(current.and(initialFormula), TRUE);
        }

        EquivalenceClass next = s.next.unfold(true).temporalStep(vs.pickAny()).and(initialFormula);

        // Move up... ? Check before and? for initialFormula and next
        if (impCover && succ.implies(next)) {
            return new DetLimitSlaveState(succ, TRUE);
        }

        return new DetLimitSlaveState(succ, next);
    }

    @Override
    protected Set<ValuationSet> generateSuccTransitions(DetLimitSlaveState s) {
        return valuationSetFactory.createAllValuationSets();
    }
}

class DetLimitSlaveState {
    protected final EquivalenceClass current;
    protected final EquivalenceClass next;

    DetLimitSlaveState(EquivalenceClass current, EquivalenceClass next) {
        this.current = current;
        this.next = next;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DetLimitSlaveState that = (DetLimitSlaveState) o;
        return Objects.equals(current, that.current) &&
                Objects.equals(next, that.next);
    }

    @Override
    public int hashCode() {
        return Objects.hash(current, next);
    }

    @Override
    public String toString() {
        return "{" + Simplifier.simplify(current.getRepresentative(), Simplifier.Strategy.PROPOSITIONAL) + ", "
                + Simplifier.simplify(next.getRepresentative(), Simplifier.Strategy.PROPOSITIONAL) + "}";
    }
}
