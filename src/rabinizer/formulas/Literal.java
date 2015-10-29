package rabinizer.formulas;

import rabinizer.bdd.Valuation;

import java.util.ArrayList;

import com.microsoft.z3.*;

import net.sf.javabdd.*;
import rabinizer.bdd.BDDForFormulae;

public class Literal extends FormulaNullary {

    public String atom;
    public int atomId;
    public boolean negated;

    public Literal(String atom, int atomId, boolean negated) {
        this.atom = atom;
        this.atomId = atomId;
        this.negated = negated;
    }

    @Override
    public String operator() {
        return null;
    }

    public Literal positiveLiteral() {
        return new Literal(this.atom, this.atomId, false);
    }

    public Literal negated() {
        return new Literal(atom, atomId, !negated);
    }

    @Override
    public BDD bdd() { 
        if (cachedBdd == null) { 
            int bddVar = BDDForFormulae.bijectionBooleanAtomBddVar.id(this.positiveLiteral()); // R3: just "this"
            if (BDDForFormulae.bddFactory.varNum() <= bddVar) {
                BDDForFormulae.bddFactory.extVarNum(1);
            }
            cachedBdd = (negated ? BDDForFormulae.bddFactory.nithVar(bddVar) : BDDForFormulae.bddFactory.ithVar(bddVar));
            BDDForFormulae.representativeOfBdd(cachedBdd, this);
        } 
        return cachedBdd;
    }

    @Override
    public int hashCode() {
        return atomId;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Literal)) {
            return false;
        } else {
            return ((Literal) o).atomId == this.atomId && ((Literal) o).negated == this.negated;
        }
    }

    @Override
    public String toReversePolishString() {
        return cachedString = (negated ? "! " : "") + atom;
    }

    @Override
    public String toString() {
        if (cachedString == null) {
            cachedString = (negated ? "!" : "") + atom;
        }
        return cachedString;
    }

    @Override
    public Formula evaluateValuation(Valuation valuation) {
        return new BooleanConstant(valuation.get(atomId) ^ negated);
    }

    @Override
    public Formula evaluateLiteral(Literal literal) {
        if (literal.atomId != this.atomId) {
            return this;
        } else {
            return new BooleanConstant(literal.negated == this.negated);
        }
    }

    @Override
    public Formula negationToNNF() {
        return this.negated();
    }

    @Override
    public Literal getAnUnguardedLiteral() {
        return this;
    }
    
    public BoolExpr toExpr(Context ctx){
    	if(negated){
    		return ctx.mkNot((BoolExpr) ctx.mkBoolConst(atom));
    	}else{
    		return (BoolExpr) ctx.mkBoolConst(atom);
    	}
    }

	@Override
	public String toZ3String(boolean is_atom) {
		if(is_atom){
			return (negated?"!":"")+atom;
		}else{
			if(negated){
				return "(not "+atom+" )";
			}else{
				return atom;
			}
		}
	}

	@Override
	public ArrayList<String> getAllPropositions() {
		ArrayList<String> a=new ArrayList<String>();
		a.add(atom);
		return a;
	}

	@Override
	public Formula rmAllConstants() {
		negated=!negated;
		Formula result=negated();
		negated=!negated;
		return result;
		
	}

	@Override
	public Formula simplifyLocally() {
		return this;
	}

}
