package rabinizer.ltl;

import rabinizer.ltl.bdd.BDDEquivalenceClassFactory;
import rabinizer.ltl.bdd.BDDValuationSetFactory;
import rabinizer.ltl.z3.Z3EquivalenceClassFactory;
import rabinizer.ltl.z3.Z3ValuationSetFactory;

import java.util.Set;

public class FactoryRegistry {

    public final static Backend defaultBackend = Backend.BDD;

    public static EquivalenceClassFactory createEquivalenceClassFactory(Set<Formula> domain) {
        return createEquivalenceClassFactory(defaultBackend, domain);
    }

    public static EquivalenceClassFactory createEquivalenceClassFactory(Backend backend, Set<Formula> domain) {
        try {
            switch (backend) {
                case Z3:
                    return new Z3EquivalenceClassFactory(domain);

                case BDD:
                default:
                    return new BDDEquivalenceClassFactory(domain);
            }
        } catch (Exception e) {
            System.err.println("Unable to instantiate factory with " + backend + " backend. Falling back to the BDD backend. (" + e + ")");
            return new BDDEquivalenceClassFactory(domain);
        }
    }

    public static ValuationSetFactory<String> createValuationSetFactory(Set<String> domain) {
        return createValuationSetFactory(defaultBackend, domain);
    }

    public static ValuationSetFactory<String> createValuationSetFactory(Backend backend, Set<String> domain) {
        try {
            switch (backend) {
                case Z3:
                    return new Z3ValuationSetFactory(domain);

                case BDD:
                default:
                    return new BDDValuationSetFactory(domain);
            }
        } catch (Exception e) {
            System.err.println("Unable to instantiate factory with " + backend + " backend. Falling back to the BDD backend. (" + e + ")");
            return new BDDValuationSetFactory(domain);
        }
    }

    public enum Backend {
        BDD, Z3
    }
}
