package rabinizer.formulas;

import rabinizer.bdd.Valuation;

import java.util.ArrayList;

import com.microsoft.z3.*;

import net.sf.javabdd.*;
import rabinizer.bdd.BDDForFormulae;

public class Literal extends FormulaNullary {

    final String atom;
    final int atomId;
    final boolean negated;

    private final int cachedHash;

    Literal(String atom, int atomId, boolean negated,long id) {
    	super(id);
        this.atom         = atom;
        this.atomId       = atomId;
        this.negated      = negated;
        this.cachedHash   = init_hash();
    }

    @Override
    public String operator() {
        return null;
    }
    
    public boolean getNegated(){
    	return negated;
    }
    public Literal positiveLiteral() {
        return (Literal) FormulaFactory.mkLit(this.atom, this.atomId, false);
    }

    public Literal negated() {
    	return (Literal) FormulaFactory.mkLit(atom, atomId, !negated);
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
    public int hashCode(){
    	return cachedHash;
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
        return FormulaFactory.mkConst(valuation.get(atomId) ^ negated);
    }

    @Override
    public Formula evaluateLiteral(Literal literal) {
        if (literal.atomId != this.atomId) {
            return this;
        } else {
            return FormulaFactory.mkConst(literal.negated == this.negated);
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
    	
    	if(cachedLTL==null){
    		cachedLTL=ctx.mkBoolConst(atom);
    		if(negated){
    			cachedLTL=ctx.mkNot(cachedLTL);
    		}
    	}
    	return cachedLTL;
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
		return FormulaFactory.mkLit(getAtom(), atomId, negated);
		
	}

	public String getAtom() {
		return atom;
	}

	@Override
	public Formula setToConst(long id, boolean constant) {
		return (id==unique_id?FormulaFactory.mkConst(constant) : this);
	}


	
	private int init_hash(){
		return ((atom.hashCode() % 34483) *32363)+(negated? 97 : 167) % 999983;
	}

	@Override
	public Formula acceptFormula(Formula_Visitor v) {
		return v.visitL(this);
	}

	@Override
	public boolean acceptBool(Attribute_Visitor v) {
		return v.visitL(this);
	}

	@Override
	public boolean acceptBinarybool(Attribute_Binary_Visitor v, Formula f) {
		return v.visitL(this, f);
	}


}
