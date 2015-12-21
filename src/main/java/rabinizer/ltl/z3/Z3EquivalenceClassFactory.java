package rabinizer.ltl.z3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import rabinizer.ltl.BooleanConstant;
import rabinizer.ltl.EquivalenceClass;
import rabinizer.ltl.EquivalenceClassFactory;
import rabinizer.ltl.Formula;
import com.microsoft.z3.BoolExpr;

public class Z3EquivalenceClassFactory extends Z3LibraryWrapper<Formula> implements EquivalenceClassFactory {

    private Collection<Z3EquivalenceClass> alreadyUsed;

    public Z3EquivalenceClassFactory(Set<Formula> domain) {
        super(domain);
        alreadyUsed = new ArrayList<Z3EquivalenceClass>();
    }

    @Override
    public EquivalenceClass getTrue() {
        return probe(new Z3EquivalenceClass(BooleanConstant.TRUE, createZ3(BooleanConstant.TRUE), this));
    }

    @Override
    public EquivalenceClass getFalse() {
        return probe(new Z3EquivalenceClass(BooleanConstant.FALSE, createZ3(BooleanConstant.FALSE), this));
    }

    @Override
    public EquivalenceClass createEquivalenceClass(Formula formula) {
        return probe(new Z3EquivalenceClass(formula, createZ3(formula), this));
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
