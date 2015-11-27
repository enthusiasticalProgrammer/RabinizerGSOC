package rabinizer.formulas;

import java.util.ArrayList;
import java.util.HashMap;

public class FormulaFactory {

    private static HashMap<Formula, Formula> formulae = new HashMap<>();
    private static long next_identifier = 0L;

    private static SimplifyBooleanVisitor get_vis() {
        return SimplifyBooleanVisitor.getVisitor();
    }


    //For and/or, the output of the formula can be reverted
    // because this makes Comparable easier
    public static Formula mkAnd(Formula... af) {
        Formula swap;
        ArrayList<Formula> helper = new ArrayList<>();
        for (Formula anAf : af) {
            if (anAf instanceof Conjunction) {
                helper.addAll(((FormulaBinaryBoolean) anAf).children);
            } else {
                helper.add(anAf);
            }
        }

        for (int i = 0; i < helper.size(); i++) {
            for (int j = helper.size() - 1; j > i; j--) {
                if (helper.get(i).unique_id > helper.get(j).unique_id) {
                    swap = helper.get(i);
                    helper.set(i, helper.get(j));
                    helper.set(j, swap);
                } else if (helper.get(i).unique_id == helper.get(j).unique_id) {
                    helper.remove(j);
                }
            }
        }

        if (helper.isEmpty()) {
            return FormulaFactory.mkConst(true);
        } else if (helper.size() == 1) {
            return helper.get(0);
        }

        Formula z = (new Conjunction(helper, next_identifier++)).acceptFormula(get_vis());
        return probe(z);

    }

    public static Formula mkOr(Formula... af) {
        Formula swap;
        ArrayList<Formula> helper = new ArrayList<>();

        for (Formula anAf : af) {
            if (anAf instanceof Disjunction) {
                helper.addAll(((FormulaBinaryBoolean) anAf).children);
            } else {
                helper.add(anAf);
            }
        }
        for (int i = 0; i < helper.size(); i++) {
            for (int j = helper.size() - 1; j > i; j--) {
                if (helper.get(i).unique_id > helper.get(j).unique_id) {
                    swap = helper.get(i);
                    helper.set(i, helper.get(j));
                    helper.set(j, swap);
                } else if (helper.get(i).unique_id == helper.get(j).unique_id) {
                    helper.remove(j);
                }
            }
        }
        if (helper.isEmpty()) {
            return FormulaFactory.mkConst(false);
        } else if (helper.size() == 1) {
            return helper.get(0);
        }
        Formula z = (new Disjunction(helper, next_identifier++)).acceptFormula(get_vis());
        return probe(z);

    }

    public static Formula mkAnd(ArrayList<Formula> af) {
        Formula[] helper = new Formula[af.size()];
        helper = af.toArray(helper);
        return mkAnd(helper);
    }

    public static Formula mkOr(ArrayList<Formula> af) {
        Formula[] helper = new Formula[af.size()];
        helper = af.toArray(helper);
        return mkOr(helper);
    }


    public static Formula mkConst(boolean t) {
        Formula z = new BooleanConstant(t, next_identifier++);
        return probe(z);
    }

    public static Formula mkF(Formula child) {
        Formula z = new FOperator(child, next_identifier++).acceptFormula(get_vis());
        return probe(z);
    }

    public static Formula mkG(Formula child) {
        Formula z = new GOperator(child, next_identifier++).acceptFormula(get_vis());
        return probe(z);
    }

    public static Formula mkLit(String proposition, int atomId, boolean negated) {
        Formula z = new Literal(proposition, atomId, negated, next_identifier++);
        return probe(z);
    }

    public static Formula mkNot(Formula child) {
        Formula z = new Negation(child, next_identifier++).acceptFormula(get_vis());
        return probe(z);
    }

    public static Formula mkU(Formula l, Formula r) {
        Formula z = new UOperator(l, r, next_identifier++).acceptFormula(get_vis());
        return probe(z);

    }

    public static Formula mkX(Formula child) {
        Formula z = new XOperator(child, next_identifier++).acceptFormula(get_vis());
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
}
