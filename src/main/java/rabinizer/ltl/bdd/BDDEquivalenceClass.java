package rabinizer.ltl.bdd;

import net.sf.javabdd.BDD;
import rabinizer.ltl.*;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class BDDEquivalenceClass implements EquivalenceClass {

    private final BDD bdd;
    private final BDDEquivalenceClassFactory factory;
    private Formula representative;

    BDDEquivalenceClass(Formula representative, BDD bdd, BDDEquivalenceClassFactory factory) {
        this.representative = representative;
        this.bdd = bdd;
        this.factory = factory;
    }

    @Override
    public Formula getRepresentative() {
        if (representative == null) {
            representative = factory.createRepresentative(bdd);
        }

        return representative;
    }

    @Override
    public boolean implies(EquivalenceClass equivalenceClass) {
        if (!(equivalenceClass instanceof BDDEquivalenceClass)) {
            return false;
        }

        BDDEquivalenceClass that = (BDDEquivalenceClass) equivalenceClass;

        if (!this.bdd.getFactory().equals(that.bdd.getFactory())) {
            return false;
        }

        return this.bdd.imp(that.bdd).isOne();
    }

    @Override
    public boolean equivalent(EquivalenceClass equivalenceClass) {
        return this.equals(equivalenceClass);
    }

    @Override
    public EquivalenceClass unfold(boolean unfoldG) {
        return factory.unfold(this, unfoldG);
    }

    @Override
    public EquivalenceClass temporalStep(Set<String> valuation) {
        return factory.temporalStep(this, valuation);
    }

    @Override
    public EquivalenceClass and(EquivalenceClass eq) {
        if (eq instanceof BDDEquivalenceClass) {
            return new BDDEquivalenceClass(Simplifier.simplify(new Conjunction(getRepresentative(), eq.getRepresentative()), Simplifier.Strategy.PROPOSITIONAL), bdd.and(((BDDEquivalenceClass) eq).bdd), factory);
        }

        return factory.createEquivalenceClass(new Conjunction(getRepresentative(), eq.getRepresentative()));
    }

    @Override
    public EquivalenceClass or(EquivalenceClass eq) {
        if (eq instanceof BDDEquivalenceClass) {
            return new BDDEquivalenceClass(Simplifier.simplify(new Disjunction(getRepresentative(), eq.getRepresentative()), Simplifier.Strategy.PROPOSITIONAL), bdd.or(((BDDEquivalenceClass) eq).bdd), factory);
        }

        return factory.createEquivalenceClass(new Disjunction(getRepresentative(), eq.getRepresentative()));
    }

    @Override
    public boolean isTrue() {
        return bdd.isOne();
    }

    @Override
    public boolean isFalse() {
        return bdd.isZero();
    }

    @Override
    public Set<Formula> getSupport() {
        if (bdd.isZero() || bdd.isOne()) {
            return Collections.emptySet();
        }

        // TODO: Traverse bdd or fix bdd library
        try {
            Formula support = factory.createRepresentative(bdd.support());

            if (support instanceof BooleanConstant) {
                return Collections.emptySet();
            }

            if (support instanceof Conjunction) {
                return ((Conjunction) support).children;
            }

            if (support instanceof Disjunction) {
                throw new IllegalStateException();
            }

            return Collections.singleton(support);
        } catch (NullPointerException e) {
            return getRepresentative().getTopMostPropositions();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BDDEquivalenceClass that = (BDDEquivalenceClass) o;
        return Objects.equals(bdd, that.bdd) && Objects.equals(bdd.getFactory(), that.bdd.getFactory());
    }

    @Override
    public int hashCode() {
        return Objects.hash(bdd);
    }
}
