package rabinizer.automata.output;

import java.util.*;

import jhoafparser.ast.AtomAcceptance;
import jhoafparser.ast.BooleanExpression;
import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import rabinizer.automata.TranSet;
import rabinizer.collections.valuationset.ValuationSet;
import rabinizer.collections.valuationset.ValuationSetFactory;

public abstract class HOAConsumerAbstractRabin<T, C> extends HOAConsumerExtended<T,C> {

    protected final IdentityHashMap<TranSet<T>, TranSet<T>> acceptanceToUniqueAcceptance;
    protected final IdentityHashMap<TranSet<T>, Integer> acceptanceNumbers;

    public HOAConsumerAbstractRabin(HOAConsumer hoa, ValuationSetFactory valuationSetFactory, T initialState, C accCond) {
        super(hoa, valuationSetFactory, accCond);
        acceptanceToUniqueAcceptance = new IdentityHashMap<>();
        acceptanceNumbers = new IdentityHashMap<>();
        setHOAHeader(initialState.toString());
        setInitialState(initialState);
        setAcceptanceCondition();
    }

    @Override
    public abstract void setAcceptanceCondition();

    protected BooleanExpression<AtomAcceptance> mkFin(TranSet<T> tranSet) {
        int i = getTranSetId(tranSet);
        return new BooleanExpression<>(new AtomAcceptance(AtomAcceptance.Type.TEMPORAL_FIN, i, false));
    }

    protected BooleanExpression<AtomAcceptance> mkInf(TranSet<T> tranSet) {
        int i = getTranSetId(tranSet);
        return new BooleanExpression<>(new AtomAcceptance(AtomAcceptance.Type.TEMPORAL_INF, i, false));
    }

    @Override
    public void addEdge(ValuationSet key, T end) {
        List<Integer> accSets = new ArrayList<>();
        Set<ValuationSet> realEdges = getMaximallyMergedEdgesOfEdge(key);
        for (ValuationSet edgeKey : realEdges) {
            accSets.clear();
            acceptanceNumbers.keySet().stream().filter(set -> set.containsAll(currentState, edgeKey)).forEach(set -> accSets.add(acceptanceNumbers.get(set)));
            addEdgeBackend(edgeKey, end, accSets);
        }
    }

    protected Set<ValuationSet> getMaximallyMergedEdgesOfEdge(ValuationSet initialValuation) {
        Set<ValuationSet> result = new HashSet<>();
        result.add(initialValuation);

        for (TranSet<T> acceptanceCondition : acceptanceNumbers.keySet()) {
            result = splitAccordingToAcceptanceSet(result, acceptanceCondition);
        }

        return result;
    }

    protected Set<ValuationSet> splitAccordingToAcceptanceSet(Set<ValuationSet> result, TranSet<T> acceptanceCondition) {
        Set<ValuationSet> toRemove = new HashSet<>();
        Set<ValuationSet> toAdd = new HashSet<>();

        for (ValuationSet edge : result) {
            ValuationSet interestingValuationSet = acceptanceCondition.asMap().get(currentState);
            if (interestingValuationSet != null && interestingValuationSet.intersects(edge) && !interestingValuationSet.containsAll(edge)) {
                toRemove.add(edge);
                toAdd.add(edge.intersect(interestingValuationSet));
                toAdd.add(edge.intersect(interestingValuationSet.complement()));
            }
        }

        result.removeAll(toRemove);
        result.addAll(toAdd);
        return result;
    }

    protected int getTranSetId(TranSet<T> o) {
        if (!acceptanceToUniqueAcceptance.containsKey(o)) {
            for (TranSet<T> tranSet : acceptanceToUniqueAcceptance.keySet()) {
                if (tranSet.equals(o)) {
                    acceptanceToUniqueAcceptance.put(o, acceptanceToUniqueAcceptance.get(tranSet));
                    break;
                }
            }
        }

        if (!acceptanceToUniqueAcceptance.containsKey(o)) {
            acceptanceToUniqueAcceptance.put(o, o);
        }
        TranSet<T> key = acceptanceToUniqueAcceptance.get(o);
        if (acceptanceNumbers.get(key) == null) {
            acceptanceNumbers.put(key, acceptanceNumbers.keySet().size());
        }
        return acceptanceNumbers.get(key);
    }
}
