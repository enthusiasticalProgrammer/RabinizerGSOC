package rabinizer.ltl.bdd;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import net.sf.javabdd.BDD;
import rabinizer.ltl.*;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class BDDValuationSetFactory extends BDDLibraryWrapper<Literal> implements ValuationSetFactory {

    private final Set<String> alphabet;

    public BDDValuationSetFactory(Collection<String> domain) {
        super(domain.stream().map(s -> new Literal(s, false)).collect(Collectors.toSet()));
        alphabet = ImmutableSet.copyOf(domain);
    }

    @Override
    public Set<String> getAlphabet() {
        return alphabet;
    }

    @Override
    public BDDValuationSet createEmptyValuationSet() {
        return new BDDValuationSet(factory.zero(), this);
    }

    @Override
    public BDDValuationSet createUniverseValuationSet() {
        return new BDDValuationSet(factory.one(), this);
    }

    @Override
    public BDDValuationSet createValuationSet(Set<String> valuation) {
        return createValuationSet(valuation, alphabet);
    }

    @Override
    public BDDValuationSet createValuationSet(Set<String> valuation, Set<String> base) {
        Formula f = new Conjunction(base.stream().map(s -> new Literal(s, !valuation.contains(s))));
        BDD bdd = createBDD(f);
        return new BDDValuationSet(bdd, this);
    }

    @Override
    public BDDValuationSet createValuationSetSet(Set<Set<String>> set) {
        BDDValuationSet vs = createEmptyValuationSet();
        vs.addAll(set);
        return vs;
    }

    @Override
    public Set<ValuationSet> createAllValuationSets() {
        return Sets.powerSet(alphabet).stream().map(this::createValuationSet).collect(Collectors.toSet());
    }

    Set<String> pickAny(BDD bdd) {
        Set<Literal> satAssignment = getSatAssignment(bdd);
        return satAssignment.stream().filter(literal -> !literal.getNegated()).map(Literal::getAtom)
                .collect(Collectors.toSet());
    }
}
