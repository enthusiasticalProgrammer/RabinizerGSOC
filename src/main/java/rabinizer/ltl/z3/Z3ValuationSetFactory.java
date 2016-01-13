package rabinizer.ltl.z3;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.microsoft.z3.BoolExpr;
import rabinizer.ltl.*;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Z3ValuationSetFactory extends Z3LibraryWrapper<Literal> implements ValuationSetFactory {

    Set<String> alphabet;

    public Z3ValuationSetFactory(Set<String> domain) {
        super(domain.stream().map(s -> new Literal(s, false)).collect(Collectors.toSet()));
        alphabet = ImmutableSet.copyOf(domain);
    }

    @Override
    public Set<String> getAlphabet() {
        return new HashSet<>(alphabet);
    }

    @Override
    public Z3ValuationSet createEmptyValuationSet() {
        return new Z3ValuationSet(this, createZ3(BooleanConstant.FALSE));
    }

    @Override
    public Z3ValuationSet createUniverseValuationSet() {
        return new Z3ValuationSet(this, createZ3(BooleanConstant.TRUE));
    }

    @Override
    public Z3ValuationSet createValuationSet(Set<String> valuation) {
        Formula f = new Conjunction(alphabet.stream().map(s -> new Literal(s, !valuation.contains(s))));
        return new Z3ValuationSet(this, createBoolExpr(f));
    }

    @Override
    public Z3ValuationSet createValuationSetSet(Set<Set<String>> set) {
        Z3ValuationSet vs = createEmptyValuationSet();
        vs.addAll(set);
        return vs;
    }

    @Override
    public ValuationSet createValuationSet(Set<String> valuation, Set<String> base) {
        return null;
    }

    @Override
    public Set<ValuationSet> createAllValuationSets() {
        return Sets.powerSet(alphabet).stream().map(this::createValuationSet).collect(Collectors.toSet());
    }

    private BoolExpr createBoolExpr(Formula f) {
        return super.createZ3(f);
    }

    Set<String> pickAny(BoolExpr bool) {
        Set<Literal> satAssignment = getSatAssignment(bool);
        return satAssignment.stream().filter(literal -> !literal.getNegated()).map(Literal::getAtom)
                .collect(Collectors.toSet());
    }

}