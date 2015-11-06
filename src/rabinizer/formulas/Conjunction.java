package rabinizer.formulas;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;

import net.sf.javabdd.BDD;
import rabinizer.bdd.BDDForFormulae;
import rabinizer.exec.Main;


/**
 * @author Andreas Gaiser & Ruslan Ledesma-Garza & Christopher Ziegler
 *
 *
 */
public class Conjunction extends FormulaBinaryBoolean {


	private final int cachedHash;
	
    Conjunction(List<Formula> af, long id) {
        super(af,id);
        this.cachedHash = init_hash();
    }
        

	@Override
    public Formula ThisTypeBoolean(ArrayList<Formula> af) {
        return FormulaFactory.mkAnd(af);
    }

    @Override
    public String operator() {
        return "&";
    }

    
    @Override
    public BDD bdd() {
    	if (cachedBdd == null) {
    		cachedBdd = BDDForFormulae.bddFactory.one();
    		for(Formula child : children){
    			cachedBdd = cachedBdd.and(child.bdd());
    		}
    		BDDForFormulae.representativeOfBdd(cachedBdd, this);
    	}
    	return cachedBdd;
    }


    @Override
    public int hashCode(){
    	return cachedHash;
    }
    
    
    @Override
    public Formula removeConstants() {
    	ArrayList<Formula> new_children=new ArrayList<Formula>();
    	for(Formula child:children){
    		Formula new_child=child.removeConstants();
    		if(new_child instanceof BooleanConstant){
    			if(!((BooleanConstant) new_child).get_value()){
    				return FormulaFactory.mkConst(false);
    			}
    		}else{
    			new_children.add(new_child);
    		}
    	}
    	if(new_children.size()==1){
    		return new_children.get(0);
    	}else{
    		return FormulaFactory.mkAnd(new_children);
    	}
    	
    }

    @Override
    public boolean ignoresG(Formula f) {
//        if (!left.isVeryDifferentFrom(right)) {
//            //System.out.println("$$$$$$not very different$$$$"+this);
//            return false;
//        }
//        if (left.equals(f.unfold()) && right.isUnfoldOfF() && !right.containsG()
//            || right.equals(f.unfold()) && left.isUnfoldOfF() && !left.containsG()) {
//            //System.out.println("$$$$$$"+left+"$$$$"+right+"$$$susp "+f);
//            return true;					// independent waiting formula
//        } else {
//            return left.ignoresG(f) && right.ignoresG(f); 	// don't know yet
//        }
    	Main.verboseln("in ignoresG(Conjunction), me: "+this);
    	boolean isTransientwrt=!hasSubformula(f);
    	Main.verboseln("i don't have the subformula: "+isTransientwrt);
    	boolean result=true;
    	for(int i=0;i<children.size();i++){
    		for(int j=i+1;j<children.size();j++){
    			result=result&&(children.get(i).isTransientwrt(children.get(j))|| children.get(j).isTransientwrt(children.get(i)));
    		}
    	}
    	result=result||isTransientwrt;
        if(result){
        	return true;
        } else {	// don't know yet
        	isTransientwrt=true;
        	for(Formula child:children)
            isTransientwrt=isTransientwrt && child.ignoresG(f); 	
        	return isTransientwrt;
        }
        
    }

    @Override
    public Formula toNNF() {
    	ArrayList<Formula> nnf=new ArrayList<Formula>();
    	for(Formula child: children){
    		nnf.add(child.toNNF());
    	}
        return FormulaFactory.mkAnd(nnf);
    }

    @Override
    public Formula negationToNNF() {
    	ArrayList<Formula> negnnf=new ArrayList<Formula>();
    	for(Formula child: children){
    		negnnf.add(child.negationToNNF());
    	}
        return FormulaFactory.mkOr(negnnf);
    }
    
    public BoolExpr toExpr(Context ctx){
    	if(cachedLTL==null){
    		ArrayList<BoolExpr> exprs=new ArrayList<BoolExpr>();
    		for(Formula child: children){
    			exprs.add(child.toExpr(ctx));
    		}
    		BoolExpr[] helper=new BoolExpr[exprs.size()];
    		exprs.toArray(helper);
    		cachedLTL=ctx.mkAnd(helper);
    	}
    	return cachedLTL;
    }

    
	@Override
	public String toZ3String(boolean is_atom) {
		ArrayList<String> al=new ArrayList<String>();
		for(Formula child: children){
			al.add(child.toZ3String(is_atom));
		}
		
		String result="";
		if(is_atom){
			for(String prop: al){
				if(prop.equals("false")){
					return "false";
				}else if(!prop.equals("true")){
					result=result+(result.equals("")?prop :" &"+prop);
				}
			}
			if(result==""){
				return "true";
			}else{
				return result;
			}
		}else{
			result="(and ";
			for(String prop: al){
				if(prop.equals("false")){
					return "false";
				}else if(!prop.equals("true")){
					result=result+prop+" ";
				}
			}
			if(result.equals("(and ")){
				return "true";
			}else{
				return result+" )";
			}
		}
		
	}


	@Override
	public Formula rmAllConstants() {
		ArrayList<Formula> new_children=new ArrayList<Formula>();
		Formula fm;
		for(Formula child: children){
			fm=child.rmAllConstants();
			if(fm instanceof BooleanConstant){
				if(!((BooleanConstant) fm).get_value()){
					return FormulaFactory.mkConst(false);
				}
			}else{
				new_children.add(fm);
			}
		}
		if(new_children.size()==0){
			return FormulaFactory.mkConst(true);
		}else if(new_children.size()==1){
			return new_children.get(0);
		}else{
			return FormulaFactory.mkAnd(new_children);
		}
	}

	@Override
	public Formula simplifyLocally() {
		//first of all, get all subformulae beyound Conjunction(e.g. for c and (a and b)
		//I want a,b, and c, because you can simplify it more
		
		ArrayList<Formula> list=getAllChildrenOfConjunction();
		ArrayList<Formula> helper=new ArrayList<Formula>();
		for(int i=0;i<list.size();i++){
			list.set(i,list.get(i).simplifyLocally());
		}
		
		
		//should be already simplified
		//simplify formulae
		/*for(int i=0;i<list.size();i++){
			list.set(i,list.get(i).simplifyLocally());
		}*/
		
		//remove dublicates
		/*for(int i=0;i<list.size();i++){
			for(int j=list.size()-1;j>i;j--){
				if(list.get(i).get_id()==list.get(j).get_id()){
					list.remove(j);
				}
			}
		}*/
		
		
		for(int i=list.size()-1;i>=0;i--){
			if(list.get(i) instanceof BooleanConstant){	
				if(!((BooleanConstant) list.get(i)).get_value()){
					return FormulaFactory.mkConst(false);
				}
				list.remove(i);
			}
		}
		
		
						
		//put all Literals together (and check for trivial tautologies/contradictions like a and a /a and !a
		for(int i=list.size()-1;i>=0;i--){
			if(list.get(i) instanceof Literal){
				helper.add(list.get(i));
				list.remove(i);
				
			}
		}
		for(int i=0;i<helper.size();i++){
			
			for(int j=helper.size()-1;j>i;j--){
				if(((Literal) helper.get(i)).atom.equals(((Literal) helper.get(j)).atom)){
					if(((Literal) helper.get(i)).negated==(((Literal) helper.get(j)).negated)){
						helper.remove(j);
					}else{
						return FormulaFactory.mkConst(false);
					}
				}
			}
		}
		list.addAll(helper);
		
		if(list.size()==0){
			return FormulaFactory.mkConst(true);
		}else if(list.size()==1){
			return list.get(0);
		}else{
			//compare list and children and only make a new con-
			//junction if both are different (circumventing a stackoverflow)
			if(list.size()!=children.size()){
				return FormulaFactory.mkAnd(list);
			}
			
			//Therefore list has to be ordered
			for(int i=0;i<list.size();i++){
				for(int j=i+1;j<list.size();j++){
					if(list.get(i).get_id()>list.get(j).get_id()){
						Formula swap=list.get(i);
						list.set(i,list.get(j));
						list.set(j, swap);
					}
				}
				
			}
			
			for(int i=0;i<list.size();i++){
				if(list.get(i).get_id()!=children.get(i).get_id()){
					return FormulaFactory.mkAnd(list);
				}
			}
			
			return this;
		}
		
	}
	
	//helps for simplifyLocally()
	private ArrayList<Formula> getAllChildrenOfConjunction(){
		ArrayList<Formula> al=new ArrayList<Formula>();
		for(Formula child: children){
			if(child instanceof Conjunction){
				al.addAll(((Conjunction) child).getAllChildrenOfConjunction());
			}else{
				al.add(child);
			}
		}
		
		//sort them according to unique_id:
		Formula swap;
		for(int i=0;i<al.size();i++){
			for(int j=0;j<al.size();j++){
				if(al.get(i).unique_id>al.get(j).unique_id){
					swap=al.get(i);
					al.set(i,al.get(j));
					al.set(j,swap);
				}
			}
		}
		
		return al;
		
	}

	private int init_hash() {
		int offset=31607;
    	int hash=1;
    	for(Formula child:children){
    		hash %= offset;
    		hash = hash*(child.hashCode()%31601);
    	}
    	
    	return (hash + 1103) % 999983;
	}

}
