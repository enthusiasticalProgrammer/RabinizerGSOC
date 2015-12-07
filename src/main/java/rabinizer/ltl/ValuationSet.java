package rabinizer.ltl;

import java.util.Set;

public interface ValuationSet extends Set<Set<String>>, Cloneable {
    ValuationSet complement();

    boolean isUniverse();

    boolean restrictWith(Literal literal);

    Set<String> pickAny();

    Formula toFormula();

    ValuationSet clone();
}
