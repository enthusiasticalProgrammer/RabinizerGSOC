package rabinizer.ltl;

public interface EquivalenceClassFactory {

    EquivalenceClass getTrue();

    EquivalenceClass getFalse();

    EquivalenceClass createEquivalenceClass(Formula formula);

}
