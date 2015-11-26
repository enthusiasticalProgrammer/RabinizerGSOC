package rabinizer.formulas;

import net.sf.javabdd.BDD;
import rabinizer.bdd.BDDForFormulae;

import java.util.ArrayList;

import com.microsoft.z3.*;

public class BooleanConstant extends FormulaNullary {


    private final boolean value;

    private final int cachedHash;

    BooleanConstant(boolean value,long id) {
    	super(id);
        this.value = value;
        this.cachedHash = init_hash();
    }

    @Override
    public String operator() {
        return null;
    }

    @Override
    public BDD bdd() {
        if (cachedBdd == null) { 
            cachedBdd = (this.value ? BDDForFormulae.bddFactory.one() : BDDForFormulae.bddFactory.zero());
            BDDForFormulae.representativeOfBdd(cachedBdd, this);
        } 
        return cachedBdd;
    }

    @Override
    public int hashCode() {
        return cachedHash;
    }
    
    public boolean get_value() {
    	return value;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BooleanConstant)) {
            return false;
        } else {
            return ((BooleanConstant) o).value == value;
        }
    }

    @Override
    public String toReversePolishString() {
        return toString();
    }

    @Override
    public String toString() {
        if (cachedString == null) {
            cachedString = (value ? "true" : "false");
        }
        return cachedString;
    }

    @Override
    public Formula negationToNNF() {
        return FormulaFactory.mkConst(!value);
    }
    
    public BoolExpr toExpr(Context ctx){
    	if(cachedLTL==null){
    		cachedLTL=(value? ctx.mkTrue() : ctx.mkFalse());
    	}
    	return cachedLTL;
    		
    }

	@Override
	public String toZ3String(boolean is_atom) {
		return (value ? "true" : "false");
	}

	@Override
	public ArrayList<String> getAllPropositions() {
		return new ArrayList<String>();
	}

	@Override
	public Formula rmAllConstants() {
		return FormulaFactory.mkConst(value);
	}

	@Override
	public Formula setToConst(long id, boolean constant) {
		return this;
	}

	
	private int init_hash() {
		return value ? 1 : 2;
	}

	@Override
	public Formula acceptFormula(Formula_Visitor v) {
		return v.visitB(this);
	}

	@Override
	public boolean acceptBool(Attribute_Visitor v) {
		return v.visitB(this);
	}

	@Override
	public boolean acceptBinarybool(Attribute_Binary_Visitor v, Formula f) {
		return v.visitB(this, f);
	}

}
