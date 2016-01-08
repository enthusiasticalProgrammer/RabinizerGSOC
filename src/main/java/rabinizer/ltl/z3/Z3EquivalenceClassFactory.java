package rabinizer.ltl.z3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import rabinizer.ltl.*;
import com.microsoft.z3.BoolExpr;

public class Z3EquivalenceClassFactory extends Z3LibraryWrapper<Formula> implements EquivalenceClassFactory {

    private Collection<Z3EquivalenceClass> alreadyUsed;

    public Z3EquivalenceClassFactory(Set<Formula> domain) {
        super(domain);
        alreadyUsed = new ArrayList<>();
    }

    @Override
    public EquivalenceClass getTrue() {
        return createEquivalenceClass(BooleanConstant.TRUE);
    }

    @Override
    public EquivalenceClass getFalse() {
        return createEquivalenceClass(BooleanConstant.FALSE);
    }

    @Override
    public EquivalenceClass createEquivalenceClass(Formula formula) {
        Formula simplifiedFormula = Simplifier.simplify(formula, Simplifier.Strategy.PROPOSITIONAL);
        return probe(new Z3EquivalenceClass(simplifiedFormula, createZ3(simplifiedFormula), this));
    }

    private Z3EquivalenceClass probe(Z3EquivalenceClass newClass) {
        for (Z3EquivalenceClass oldClass : alreadyUsed) {
            if (checkEquality(oldClass.expression, newClass.expression)) {
                return oldClass;
            }
        }
        alreadyUsed.add(newClass);
        return newClass;
    }

}
