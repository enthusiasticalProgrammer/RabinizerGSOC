package rabinizer.ltl.bdd;

import net.sf.javabdd.BDD;
import rabinizer.ltl.Conjunction;
import rabinizer.ltl.Disjunction;
import rabinizer.ltl.EquivalenceClass;
import rabinizer.ltl.Formula;

import java.util.Objects;
import java.util.Set;

public class BDDEquivalenceClass implements EquivalenceClass {

    private final BDD bdd;
    private final BDDEquivalenceClassFactory factory;
    private Formula representative;
    private Formula simplifiedRepresentative;

    BDDEquivalenceClass(Formula representative, BDD bdd, BDDEquivalenceClassFactory factory) {
        this.representative = representative;
        this.bdd = bdd;
        this.factory = factory;
    }

    @Override
    public Formula getRepresentative() {
        if (representative == null) {
            representative = getSimplifiedRepresentative();
        }

        return representative;
    }

    @Override
    public Formula getSimplifiedRepresentative() {
        if (simplifiedRepresentative == null) {
            simplifiedRepresentative = factory.createRepresentative(bdd);
        }

        return simplifiedRepresentative;
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
        return factory.createEquivalenceClass(getRepresentative().unfold(unfoldG));
    }

    @Override
    public EquivalenceClass temporalStep(Set<String> valuation) {
        return factory.createEquivalenceClass(getRepresentative().temporalStep(valuation));
    }

    @Override
    public EquivalenceClass and(EquivalenceClass eq) {
        if (eq instanceof BDDEquivalenceClass) {
            return new BDDEquivalenceClass(new Conjunction(getRepresentative(), eq.getRepresentative()), bdd.and(((BDDEquivalenceClass) eq).bdd), factory);
        }

        return factory.createEquivalenceClass(new Conjunction(getRepresentative(), eq.getRepresentative()));
    }

    @Override
    public EquivalenceClass or(EquivalenceClass eq) {
        if (eq instanceof BDDEquivalenceClass) {
            return new BDDEquivalenceClass(new Disjunction(getRepresentative(), eq.getRepresentative()), bdd.or(((BDDEquivalenceClass) eq).bdd), factory);
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
