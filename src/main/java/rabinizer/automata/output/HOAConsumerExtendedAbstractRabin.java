package rabinizer.automata.output;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jhoafparser.ast.AtomAcceptance;
import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import rabinizer.automata.TranSet;
import rabinizer.collections.valuationset.ValuationSet;
import rabinizer.collections.valuationset.ValuationSetFactory;

public abstract class HOAConsumerExtendedAbstractRabin<T, C> extends HOAConsumerExtended<T, C> {

    public HOAConsumerExtendedAbstractRabin(HOAConsumer hoa, ValuationSetFactory valuationSetFactory) {
        super(hoa, valuationSetFactory);
    }

    protected AtomAcceptance mkInf(TranSet tranSet) {
        return mkInf(getTranSetId(tranSet));
    }

    protected AtomAcceptance mkFin(TranSet tranSet) {
        int i = getTranSetId(tranSet);
        return new AtomAcceptance(AtomAcceptance.Type.TEMPORAL_FIN, i, false);
    }

    public void addEdge(T begin, ValuationSet key, T end) throws HOAConsumerException {
        List<Integer> accSets = new ArrayList<Integer>();
        Set<ValuationSet> realEdges = getMaximallyMergedEdgesOfEdge(begin, key);
        for (ValuationSet edgeKey : realEdges) {
            accSets.clear();
            acceptanceNumbers.keySet().stream().filter(set -> set.containsAll(begin, edgeKey)).forEach(set -> accSets.add(acceptanceNumbers.get(set)));
            addEdgeBackend(begin, edgeKey.toFormula(), end, accSets);
        }
    }

    protected Set<ValuationSet> getMaximallyMergedEdgesOfEdge(T begin, ValuationSet initialValuation) {
        Set<ValuationSet> result = new HashSet<ValuationSet>();
        result.add(initialValuation);

        for (TranSet<T> acceptanceCondition : acceptanceNumbers.keySet()) {
            result = splitAccordingToAcceptanceSet(begin, result, acceptanceCondition);
        }
        return result;
    }

    protected Set<ValuationSet> splitAccordingToAcceptanceSet(T begin, Set<ValuationSet> result, TranSet<T> acceptanceCondition) {
        Set<ValuationSet> toRemove = new HashSet<ValuationSet>();
        Set<ValuationSet> toAdd = new HashSet<ValuationSet>();
        for (ValuationSet edge : result) {
            ValuationSet interestingValuationSet = acceptanceCondition.asMap().get(begin);
            if (interestingValuationSet != null && interestingValuationSet.intersects(edge) && !interestingValuationSet.containsAll(edge)) {
                toRemove.add(edge);
                toAdd.add(edge.intersect(interestingValuationSet));
                toAdd.add(edge.intersect(interestingValuationSet).complement());
            }
        }
        result.removeAll(toRemove);
        result.addAll(toAdd);
        return result;
    }
}
