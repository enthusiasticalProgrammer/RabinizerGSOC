package rabinizer.ltl.z3;

import com.microsoft.z3.BoolExpr;
import rabinizer.ltl.Conjunction;
import rabinizer.ltl.Disjunction;
import rabinizer.ltl.EquivalenceClass;
import rabinizer.ltl.Formula;

import java.util.Set;

public class Z3EquivalenceClass implements EquivalenceClass {

    final BoolExpr expression;
    private final Z3EquivalenceClassFactory factory;
    private Formula representative;

    Z3EquivalenceClass(Formula representative, BoolExpr expression, Z3EquivalenceClassFactory factory) {
        this.representative = representative;
        this.expression = expression;
        this.factory = factory;
    }

    @Override
    public Formula getRepresentative() {
        if (representative == null) {
            representative = factory.createRepresentative(((BoolExpr) expression.simplify()));
        }
        return representative;
    }

    @Override
    public boolean implies(EquivalenceClass equivalenceClass) {
        if (!(equivalenceClass instanceof Z3EquivalenceClass)) {
            return false;
        }

        Z3EquivalenceClass that = (Z3EquivalenceClass) equivalenceClass;
        return factory.checkImplies(this.expression, that.expression);
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
        return factory.createEquivalenceClass(new Conjunction(getRepresentative(), eq.getRepresentative()));
    }

    @Override
    public EquivalenceClass or(EquivalenceClass eq) {
        return factory.createEquivalenceClass(new Disjunction(getRepresentative(), eq.getRepresentative()));
    }

    @Override
    public boolean isTrue() {
        return factory.isTautology(expression);
    }

    @Override
    public boolean isFalse() {
        return factory.isContradiction(expression);
    }

    @Override
    public Set<Formula> getSupport() {
        return null;
    }
}
