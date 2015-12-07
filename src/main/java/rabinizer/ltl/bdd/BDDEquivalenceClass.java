package rabinizer.ltl.bdd;

import net.sf.javabdd.BDD;
import rabinizer.ltl.EquivalenceClass;
import rabinizer.ltl.Formula;

import java.util.Objects;

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
