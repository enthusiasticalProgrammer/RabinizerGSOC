package rabinizer.formulas;

import java.util.ArrayList;
import java.util.Arrays;

import com.microsoft.z3.*;
import net.sf.javabdd.BDD;
import rabinizer.bdd.BDDForFormulae;

public class Negation extends FormulaUnary {

    @Override
    public String operator() {
        return "!";
    }

    Negation(Formula f,long id) {
        super(f,id);
    }

    public Formula ThisTypeUnary(Formula operand) {
        return FormulaFactory.mkNot(operand);
    }

    public BDD bdd() {            // negation of ATOMIC PROPOSITIONS only
        if (cachedBdd == null) {
            Formula booleanAtom = FormulaFactory.mkNot(operand.representative());
            int bddVar = BDDForFormulae.bijectionBooleanAtomBddVar.id(booleanAtom);
            if (BDDForFormulae.bddFactory.varNum() <= bddVar) {
                BDDForFormulae.bddFactory.extVarNum(1);
            }
            cachedBdd = BDDForFormulae.bddFactory.ithVar(bddVar);
            BDDForFormulae.representativeOfBdd(cachedBdd, this);
        }
        return cachedBdd;
    }
    /*
     @Override
     public int hashCode() {
     return operand.hashCode() * 2;
     }

     @Override
     public boolean equals(Object o) {
     if (!(o instanceof Negation)) {
     return false;
     } else {
     return ((Negation) o).operand.equals(this.operand);
     }
     }

     @Override
     public Formula negated() {
     return new Negation(operand.negated());
     }

     @Override
     public String toReversePolishString() {
     return "! " + operand.toReversePolishString();
     }

     @Override
     public String toString() {
     if (cachedString == null) {
     cachedString = "! " + operand.toString();
     }
     return cachedString;
     }

     @Override
     public Tuple<Set<Formula>, Set<Formula>> recurrentFormulas(boolean accumulate) {
     return operand.recurrentFormulas(accumulate);
     }

     @Override
     public Formula evaluateValuation(Valuation valuation) {
     System.out.println("neg"+this);
     Formula o = operand.evaluateValuation(valuation);
     if (!(o instanceof BooleanConstant)) {
     Rabinizer.errorMessageAndExit("Negation.evaluateCurrentAssertions: the given formula is not in Negation Normal Form.");
     }
     return o.negated();
     }

     @Override
     public Formula removeConstants() {
     return this;
     }

     @Override
     public Formula removeX() {
     return operand;
     }
     */

    @Override
    public Formula unfold() {
        throw new UnsupportedOperationException("Supported for NNF only.");
    }

    @Override
    public Formula unfoldNoG() {
        throw new UnsupportedOperationException("Supported for NNF only.");
    }
    /*
     @Override
     public boolean isProgressFormula() {
     return operand.isProgressFormula();
     }

     @Override
     public boolean hasSubformula(Formula f) {
     return this.equals(f) || operand.hasSubformula(f);
     }

     @Override
     public Formula removeXsFromCurrentBooleanAtoms() {
     if (operand.isProgressFormula()) {
     return this;
     } else {
     return operand.removeXsFromCurrentBooleanAtoms();
     }
     }

     @Override
     public Set<Formula> gSubformulas() {
     return operand.gSubformulas();
     }

     @Override
     public Set<Formula> argumentsFinsideG(boolean acc) {
     return operand.argumentsFinsideG(acc);
     }

     public boolean untilOcurrs() {
     return operand.untilOcurrs();
     }
     */
    
    public BoolExpr toExpr(Context ctx){
      	if(cachedLTL==null){
      		cachedLTL=ctx.mkNot(operand.toExpr(ctx));
      	}
    	return cachedLTL;
    }
    

    @Override
    public Formula toNNF() {
        return operand.negationToNNF();
    }

    @Override
    public Formula negationToNNF() {
        return operand.toNNF();
    }
    /*   
     @Override
     public Set<Formula> topmostGs() {
     return operand.topmostGs();
     }
     */
    /*
     public Formula toNNF() {
     if (operand instanceof Literal) {
     Literal l = new Literal((Literal) operand);
     l.negated = !l.negated;
     return l;
     } else if (operand instanceof BooleanConstant) {
     return new BooleanConstant(!((BooleanConstant) operand).value);
     } else if (operand instanceof FOperator) {
     FOperator f = (FOperator) operand;
     return new GOperator((new Negation(f.operand)).toNNF());
     } else if (operand instanceof GOperator) {
     GOperator g = (GOperator) operand;
     return new FOperator((new Negation(g.operand)).toNNF());
     } else if (operand instanceof XOperator) {
     Negation n = (Negation) operand;
     return new XOperator((new Negation(n.operand)).toNNF());
     } else if (operand instanceof Negation) {
     return operand;
     } else if (operand instanceof UOperator) {
     UOperator u = (UOperator) operand;
     Formula l = (new UOperator(new Negation(u.right), new Conjunction(new Negation(u.left), new Negation(u.right)))).toNNF();
     Formula r = (new GOperator(new Negation(u.right))).toNNF();
     return new Disjunction(l, r);
     } else if (operand instanceof Conjunction) {
     Conjunction c = (Conjunction) operand;
     Formula l = (new Negation(c.left)).toNNF();
     Formula r = (new Negation(c.right)).toNNF();
     return new Disjunction(l, r);
     } else if (operand instanceof Disjunction) {
     Disjunction d = (Disjunction) operand;
     Formula l = (new Negation(d.left)).toNNF();
     Formula r = (new Negation(d.right)).toNNF();
     return new Conjunction(l, r);
     } else {
     throw new Error("Negation.toNNF: Unknown formula: " + operand);
     }
     }
     */

    @Override
    public int hashCode(){
    	return ((operand.hashCode() % 38867) *33317) % 999983;
    }
    
	@Override
	public String toZ3String(boolean is_atom) {
		String child=operand.toZ3String(is_atom);
		if(child.equals("true")){
			return "false";
		}else if(child.equals("false")){
			return "true";
		}else if(is_atom){
			return "!"+child;
		}else{
			return "(not "+child+" )";
		}
	}

	@Override
	public ArrayList<String> getAllPropositions() {
		String child=operand.toZ3String(true);
		ArrayList<String> a=new ArrayList<String>();
		if(!child.equals("true")&& !child.equals("false")){
			a.addAll(operand.getAllPropositions());
		}
		return a;
	}

	@Override
	public Formula rmAllConstants() {
		Formula child=operand.rmAllConstants();
		if(child instanceof BooleanConstant){
			return FormulaFactory.mkConst(!((BooleanConstant) child).value);
		}
		return FormulaFactory.mkNot(child);
	}

	@Override
	public Formula simplifyLocally() {
		if(operand instanceof Negation){
			return ((Negation) operand).operand.simplifyLocally();
		}else if(operand instanceof BooleanConstant){
			return FormulaFactory.mkConst(!((BooleanConstant) operand).value);
		}else if(operand instanceof Conjunction){
			ArrayList<Formula> children=new ArrayList<Formula>();
			for(Formula child: ((Conjunction)operand).children){
				children.add(FormulaFactory.mkNot(child));
			}
			return (FormulaFactory.mkOr(children)).simplifyLocally();
		}else if(operand instanceof Disjunction){
			ArrayList<Formula> children=new ArrayList<Formula>();
			for(Formula child: ((Disjunction)operand).children){
				children.add(FormulaFactory.mkNot(child));
			}
			return (FormulaFactory.mkAnd(children)).simplifyLocally();
		}else if(operand instanceof FOperator){
			return (FormulaFactory.mkG(FormulaFactory.mkNot(((FOperator) operand).operand))).simplifyLocally();
		}else if(operand instanceof GOperator){
			return (FormulaFactory.mkF(FormulaFactory.mkNot(((GOperator) operand).operand))).simplifyLocally();
		}else if(operand instanceof Literal){
			return (((Literal) operand).negated());
		}else if(operand instanceof UOperator){
			Formula child=operand.simplifyLocally();
			if(! (child instanceof UOperator)){
				
				return FormulaFactory.mkNot(child).simplifyLocally();
			}
			return (FormulaFactory.mkOr(FormulaFactory.mkU(FormulaFactory.mkNot(((UOperator) child).right)
					,FormulaFactory.mkAnd(FormulaFactory.mkNot(((UOperator) child).left),FormulaFactory.mkNot(
							((UOperator) child).right))),FormulaFactory.mkG(
									FormulaFactory.mkNot(((UOperator) child).right)))).simplifyLocally();
		}else if(operand instanceof XOperator){
			return (FormulaFactory.mkX(FormulaFactory.mkNot(((XOperator) operand).operand))).simplifyLocally();
		}
		throw new RuntimeException("In simplifyLocally of Negation, forgot a case distinction");
	}

}
