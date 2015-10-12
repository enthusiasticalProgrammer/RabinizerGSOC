package rabinizer.formulas;

import net.sf.javabdd.BDD;
import rabinizer.bdd.BDDForFormulae;

import java.util.ArrayList;

import com.microsoft.z3.*;

public class BooleanConstant extends FormulaNullary {

    public boolean value;

    public BooleanConstant(boolean value) {
        this.value = value;
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
        return value ? 0 : 1;
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
        return new BooleanConstant(!value);
    }
    
    public BoolExpr toExpr(Context ctx){
    	if(toString().equals("true")){
    		return ctx.mkTrue();
    	}else{
    		return ctx.mkFalse();
    	}
    		
    }

	@Override
	public String toZ3String(boolean is_atom) {
		return (value ? "true" : "false");
	}

	@Override
	public ArrayList<String> getAllPropositions() {
		return new ArrayList<String>();
	}

}
