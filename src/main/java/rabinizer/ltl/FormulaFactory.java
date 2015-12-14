package rabinizer.ltl;

import java.util.*;

public class FormulaFactory {

    public static Formula mkAnd(Formula... af) {
        Set<Formula> conjuncts = new HashSet<>();

        for (Formula anAf : af) {
            if (anAf instanceof Conjunction) {
                conjuncts.addAll(((PropositionalFormula) anAf).children);
            } else {
                conjuncts.add(anAf);
            }
        }

        if (conjuncts.contains(BooleanConstant.FALSE)) {
            return BooleanConstant.FALSE;
        }

        if (conjuncts.isEmpty()) {
            return BooleanConstant.get(true);
        } else if (conjuncts.size() == 1) {
            Formula onlyOne = null;
            for (Formula a : conjuncts) {
                onlyOne = a;
            }
            return onlyOne;
        }

        return Simplifier.simplify(new Conjunction(conjuncts));
    }

    public static Formula mkOr(Formula... af) {

        Set<Formula> disjuncts = new HashSet<>();

        for (Formula anAf : af) {
            if (anAf instanceof Disjunction) {
                disjuncts.addAll(((PropositionalFormula) anAf).children);
            } else {
                disjuncts.add(anAf);
            }
        }

        if (disjuncts.isEmpty()) {
            return BooleanConstant.get(false);
        } else if (disjuncts.size() == 1) {
            Formula onlyOne = null;
            for (Formula a : disjuncts) {
                onlyOne = a;
            }
            return onlyOne;
        }
        return Simplifier.simplify(new Disjunction(disjuncts));
    }

    public static Formula mkOr(Collection<Formula> af) {
        Formula[] helper = new Formula[af.size()];
        helper = af.toArray(helper);
        return mkOr(helper);
    }

    public static Formula mkAnd(Collection<Formula> af) {
        Formula[] helper = new Formula[af.size()];
        helper = af.toArray(helper);
        return mkAnd(helper);
    }

    public static Formula mkF(Formula child) {
        return new FOperator(child);
    }

    public static Formula mkG(Formula child) {
        return new GOperator(child);
    }

    public static Formula mkLit(String proposition, boolean negated) {
        return new Literal(proposition, negated);
    }

    public static Formula mkU(Formula l, Formula r) {
        return new UOperator(l, r);
    }

    public static Formula mkX(Formula child) {
        return new XOperator(child);
    }

    public static Formula mkNot(Formula formula) {
        return formula.not();
    }
}
