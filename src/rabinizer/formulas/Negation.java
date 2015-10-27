package rabinizer.formulas;

import java.util.ArrayList;

import com.microsoft.z3.*;


public class Negation extends FormulaUnary {

    @Override
    public String operator() {
        return "!";
    }

    public Negation(Formula f) {
        super(f);
    }

    public Negation ThisTypeUnary(Formula operand) {
        return new Negation(operand);
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
      	return ctx.mkNot(operand.toExpr(ctx));
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
			return new BooleanConstant(!((BooleanConstant) child).value);
		}
		return new Negation(child);
	}

	@Override
	public Formula simplifyLocally() {
		if(operand instanceof Negation){
			return ((Negation) operand).operand.simplifyLocally();
		}else if(operand instanceof BooleanConstant){
			return new BooleanConstant(!((BooleanConstant) operand).value);
		}else if(operand instanceof Conjunction){
			return (new Disjunction(new Negation(((Conjunction)operand).left),new Negation(((Conjunction)operand).right))).simplifyLocally();
		}else if(operand instanceof Disjunction){
			return (new Conjunction(new Negation(((Disjunction)operand).left),new Negation(((Disjunction)operand).right))).simplifyLocally();
		}else if(operand instanceof FOperator){
			return (new GOperator(new Negation(((FOperator) operand).operand))).simplifyLocally();
		}else if(operand instanceof GOperator){
			return (new FOperator(new Negation(((GOperator) operand).operand))).simplifyLocally();
		}else if(operand instanceof Literal){
			return (((Literal) operand).negated());
		}else if(operand instanceof UOperator){
			Formula child=operand.simplifyLocally();
			if(! (child instanceof UOperator)){
				
				return new Negation(child).simplifyLocally();
			}
			return (new Disjunction(new UOperator(new Negation(((UOperator) child).right),new Conjunction(new Negation(((UOperator) child).left),new Negation(((UOperator) child).right))),new GOperator(new Negation(((UOperator) child).right)))).simplifyLocally();
		}else if(operand instanceof XOperator){
			return (new XOperator(new Negation(((XOperator) operand).operand))).simplifyLocally();
		}
		throw new RuntimeException("In simplifyLocally of Negation, forgot a case distinction");
	}

}
