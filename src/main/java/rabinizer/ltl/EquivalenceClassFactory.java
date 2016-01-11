package rabinizer.ltl;

public interface EquivalenceClassFactory {

    default EquivalenceClass getTrue() {
        return createEquivalenceClass(BooleanConstant.TRUE);
    }

    default EquivalenceClass getFalse() {
        return createEquivalenceClass(BooleanConstant.FALSE);
    }

    EquivalenceClass createEquivalenceClass(Formula formula);
}
