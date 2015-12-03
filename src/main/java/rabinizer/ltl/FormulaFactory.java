package rabinizer.ltl;

import java.util.*;
import java.util.stream.Collectors;

public class FormulaFactory {

    /* TODO: Merge this with simplify Visitors */
    private static Map<Formula, Formula> formulae = new HashMap<>();

    private static SimplifyBooleanVisitor getVis() {
        return SimplifyBooleanVisitor.getVisitor();
    }

    public static Formula mkAnd(Formula... af) {
        Set<Formula> conjuncts = new HashSet<Formula>();

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
            return FormulaFactory.mkConst(true);
        } else if (conjuncts.size() == 1) {
            Formula onlyOne = null;
            for (Formula a : conjuncts) {
                onlyOne = a;
            }
            return onlyOne;
        }

        Formula z = (new Conjunction(conjuncts)).accept(getVis());
        return probe(z);
    }

    public static Formula mkOr(Formula... af) {

        Set<Formula> disjuncts = new HashSet<Formula>();

        for (Formula anAf : af) {
            if (anAf instanceof Disjunction) {
                disjuncts.addAll(((PropositionalFormula) anAf).children);
            } else {
                disjuncts.add(anAf);
            }
        }

        if (disjuncts.isEmpty()) {
            return FormulaFactory.mkConst(false);
        } else if (disjuncts.size() == 1) {
            Formula onlyOne = null;
            for (Formula a : disjuncts) {
                onlyOne = a;
            }
            return onlyOne;
        }
        Formula z = (new Disjunction(disjuncts)).accept(getVis());
        return probe(z);

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

    public static Formula mkConst(boolean t) {
        Formula z = t ? BooleanConstant.TRUE : BooleanConstant.FALSE;
        return probe(z);
    }

    public static Formula mkF(Formula child) {
        Formula z = new FOperator(child).accept(getVis());
        return probe(z);
    }

    public static Formula mkG(Formula child) {
        Formula z = new GOperator(child).accept(getVis());
        return probe(z);
    }

    public static Formula mkLit(String proposition, int atomId, boolean negated) {
        Formula z = new Literal(proposition, atomId, negated);
        return probe(z);
    }

    public static Formula mkU(Formula l, Formula r) {
        Formula z = new UOperator(l, r).accept(getVis());
        return probe(z);

    }

    public static Formula mkX(Formula child) {
        Formula z = new XOperator(child).accept(getVis());
        return probe(z);
    }

    private static Formula probe(Formula z) {
        Formula inMap = formulae.get(z);
        if (inMap == null) {
            formulae.put(z, z);
            return z;
        } else {
            return inMap;
        }
    }

    public static Formula mkNot(Formula formula) {
        return probe(formula.not());
    }

}
