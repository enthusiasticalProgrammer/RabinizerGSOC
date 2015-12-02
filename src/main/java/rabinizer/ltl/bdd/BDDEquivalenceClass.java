package rabinizer.ltl.bdd;

import net.sf.javabdd.BDD;
import rabinizer.ltl.EquivalenceClass;
import rabinizer.ltl.Formula;

import java.util.Objects;

public class BDDEquivalenceClass implements EquivalenceClass {

    private final Formula representative;
    private final BDD bdd;

    BDDEquivalenceClass(Formula representative, BDD bdd) {
        this.representative = representative;
        this.bdd = bdd;
    }

    @Override
    public Formula getRepresentative() {
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
