/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rabinizer.ltl.bdd;

import net.sf.javabdd.BDD;
import rabinizer.ltl.Formula;
import rabinizer.ltl.FormulaFactory;
import rabinizer.ltl.Literal;

/**
 * @author jkretinsky
 */
public class MyBDD {

    public BDD bdd;
    public boolean valuationType; // true for valuations, false for general formulae

    public MyBDD(BDD bdd, boolean valuationType) {
        this.bdd = bdd;
        this.valuationType = valuationType;
    }

    public MyBDD and(MyBDD m) {
        return new MyBDD(this.bdd.and(m.bdd), valuationType && m.valuationType);
    }

    public MyBDD or(MyBDD m) {
        return new MyBDD(this.bdd.or(m.bdd), valuationType && m.valuationType);
    }

    public String BDDtoString() { // FIXME!!! uzavorkovani // instead toNumerical and map variableToString 
        if (bdd.isOne()) {
            return "tt";
        } else if (bdd.isZero()) {
            return "ff";
        } else {
            boolean firstOp = false;
            String result = "";
            if (bdd.high().isOne()) {
                result += variableToString(bdd.level());
                firstOp = true;
            } else if (!bdd.high().isZero()) {
                result += "(" + variableToString(bdd.level()) + "&"
                        + new MyBDD(bdd.high(), valuationType).BDDtoString() + ")";
                firstOp = true;
            }

            if (bdd.low().isOne()) {
                result += (firstOp ? "+" : "") + "!" + variableToString(bdd.level());
            } else if (!bdd.low().isZero()) {
                result += (firstOp ? "+" : "") + "(!"
                        + variableToString(bdd.level()) + "&"
                        + new MyBDD(bdd.low(), valuationType).BDDtoString() + ")";
            }
            result += "";
            return result;
        }
    }

    public String BDDtoNumericString() {
        if (bdd.isOne()) {
            return "t";
        } else if (bdd.isZero()) {
            return "f";
        } else {
            String result1 = "";
            if (bdd.high().isOne()) {
                result1 += bdd.level();
            } else if (!bdd.high().isZero()) {
                result1 += bdd.level() + "&" + new MyBDD(bdd.high(), valuationType).BDDtoNumericString();
            }

            String result2 = "";
            if (bdd.low().isOne()) {
                result2 = "!" + bdd.level();
            } else if (!bdd.low().isZero()) {
                result2 = "!" + bdd.level() + "&" + new MyBDD(bdd.low(), valuationType).BDDtoNumericString();
            }
            if (result1.isEmpty() || result2.isEmpty()) {
                return result1 + result2;
            } else {
                return "(" + result1 + "|" + result2 + ")";
            }
        }
    }

    public Formula BDDtoFormula() {
        MyBDD other = this;
        while (true) {
            if (other.bdd.isOne()) {
                return FormulaFactory.mkConst(true);
            } else if (other.bdd.isZero()) {
                return FormulaFactory.mkConst(false);
            } else if (other.bdd.high().equals(other.bdd.low())) {
                other = new MyBDD(other.bdd.high(), other.valuationType);
            } else {
                Formula high, low;
                if (other.bdd.high().isOne()) {
                    high = other.variableToFormula(bdd.level());
                } else if (other.bdd.high().isZero()) {
                    high = FormulaFactory.mkConst(false);
                } else {
                    high = FormulaFactory.mkAnd(other.variableToFormula(bdd.level()), new MyBDD(other.bdd.high(), other.valuationType).BDDtoFormula());
                }

                Formula neg;
                if (other.variableToFormula(bdd.level()) instanceof Literal) {
                    neg = ((Literal) other.variableToFormula(bdd.level())).not();
                } else {
                    neg = FormulaFactory.mkNot(other.variableToFormula(bdd.level()));
                }
                if (other.bdd.low().isOne()) {
                    low = neg;
                } else if (other.bdd.low().isZero()) {
                    low = FormulaFactory.mkConst(false);
                } else {
                    low = FormulaFactory.mkAnd(neg, new MyBDD(other.bdd.low(), other.valuationType).BDDtoFormula());
                }

                if (high.equals(FormulaFactory.mkConst(false))) {
                    return low;
                } else if (low.equals(FormulaFactory.mkConst(false))) {
                    return high;
                } else {
                    return FormulaFactory.mkOr(high, low);
                }
            }
        }
    }

    public String variableToString(int var) {
        return variableToFormula(var).toString();
    }

    public Formula variableToFormula(int var) {
        if (valuationType) {
            return FormulaFactory.mkLit(BDDForVariables.bijectionIdAtom.atom(var), var, false);
        } else {
            return BDDForFormulae.bijectionBooleanAtomBddVar.atom(var);
        }
    }
}
