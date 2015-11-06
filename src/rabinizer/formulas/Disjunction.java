package rabinizer.formulas;

import net.sf.javabdd.*;
import rabinizer.bdd.BDDForFormulae;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.z3.*;

/**
 * @author Andreas Gaiser & Ruslan Ledesma-Garza & Christopher Ziegler
 *
 *
 *
 */
public class Disjunction extends FormulaBinaryBoolean {

	private final int cachedHash;

	Disjunction(List<Formula> af, long id) {
		super(af, id);
		this.cachedHash = init_hash();
	}

	@Override
	public Formula ThisTypeBoolean(ArrayList<Formula> af) {
		return FormulaFactory.mkOr(af);
	}

	@Override
	public String operator() {
		return "|";
	}

	@Override
	public int hashCode() {
		return cachedHash;
	}

	@Override
	public Formula removeConstants() {
		ArrayList<Formula> new_children = new ArrayList<Formula>();
		for (Formula child : children) {
			Formula new_child = child.removeConstants();
			if (new_child instanceof BooleanConstant) {
				if (((BooleanConstant) new_child).get_value()) {
					return FormulaFactory.mkConst(true);
				}
			} else {
				new_children.add(new_child);
			}
		}
		if (new_children.size() == 0) {
			return FormulaFactory.mkConst(false);
		}
		if (new_children.size() == 1) {
			return new_children.get(0);
		} else {
			return FormulaFactory.mkOr(new_children);
		}
	}

	@Override
	public boolean ignoresG(Formula f) {
		// return (!left.hasSubformula(f) || left.ignoresG(f))
		// && (!right.hasSubformula(f) || right.ignoresG(f));
		if (!hasSubformula(f)) {
			return true;
		} else {
			boolean ign = true;
			for (Formula child : children) {
				ign = ign && child.ignoresG(f);
			}
			return ign;
		}
	}

	@Override
	public Formula toNNF() {
		ArrayList<Formula> new_children = new ArrayList<Formula>();
		for (Formula child : children) {
			new_children.add(child.toNNF());
		}
		return FormulaFactory.mkOr(new_children);
	}

	@Override
	public Formula negationToNNF() {
		ArrayList<Formula> new_children = new ArrayList<Formula>();
		for (Formula child : children) {
			new_children.add(child.negationToNNF());
		}
		return FormulaFactory.mkAnd(new_children);
	}

	// ============================================================
	@Override
	public boolean isUnfoldOfF() {
		for (Formula child : children) {
			if (child instanceof XOperator) {
				if (((XOperator) child).operand instanceof FOperator) {
					return true;
				}
			}
		}
		return false;
	}

	public BoolExpr toExpr(Context ctx) {
		if (cachedLTL == null) {
			ArrayList<BoolExpr> exprs = new ArrayList<BoolExpr>();
			for (Formula child : children) {
				exprs.add(child.toExpr(ctx));
			}
			BoolExpr[] helper = new BoolExpr[exprs.size()];
			exprs.toArray(helper);
			cachedLTL = ctx.mkOr(helper);
		}
		return cachedLTL;

	}

	@Override
	public String toZ3String(boolean is_atom) {
		ArrayList<String> al = new ArrayList<String>();
		for (Formula child : children) {
			al.add(child.toZ3String(is_atom));
		}

		String result = "";
		if (is_atom) {
			for (String prop : al) {
				if (prop.equals("true")) {
					return "true";
				} else if (!prop.equals("false")) {
					result = result + (result.equals("") ? prop : " &" + prop);
				}
			}
			if (result == "") {
				return "false";
			} else {
				return result;
			}
		} else {
			result = "(or ";
			for (String prop : al) {
				if (prop.equals("true")) {
					return "true";
				} else if (!prop.equals("false")) {
					result = result + prop + " ";
				}
			}
			if (result.equals("(or ")) {
				return "false";
			} else {
				return result + " )";
			}
		}
	}

	@Override
	public BDD bdd() {
		if (cachedBdd == null) {
			cachedBdd = BDDForFormulae.bddFactory.zero();
			for (Formula child : children) {
				cachedBdd = cachedBdd.or(child.bdd());
			}
			BDDForFormulae.representativeOfBdd(cachedBdd, this);
		}
		return cachedBdd;

	}

	@Override
	public Formula rmAllConstants() {
		ArrayList<Formula> new_children = new ArrayList<Formula>();
		Formula fm;
		for (Formula child : children) {
			fm = child.rmAllConstants();
			if (fm instanceof BooleanConstant) {
				if (((BooleanConstant) fm).get_value()) {
					return FormulaFactory.mkConst(true);
				}
			} else {
				new_children.add(fm);
			}
		}
		if (new_children.size() == 0) {
			return FormulaFactory.mkConst(false);
		} else if (new_children.size() == 1) {
			return new_children.get(0);
		} else {
			return FormulaFactory.mkOr(new_children);
		}
	}

	@Override
	public Formula simplifyLocally() {
		// first of all, get all subformulae beyound Conjunction(e.g. for c or
		// (a or b)
		// I want a,b, and c, because you can simplify it more

		ArrayList<Formula> list = getAllChildrenOfDisjunction();
		ArrayList<Formula> helper = new ArrayList<Formula>();

		// simplify formulae
		/*for (int i = 0; i < list.size(); i++) {
			list.set(i, list.get(i).simplifyLocally());

		}*/

		// remove dublicates
		/*
		 * for(int i=0;i<list.size();i++){ for(int j=list.size()-1;j>i;j--){
		 * if(list.get(i).get_id()==list.get(j).get_id()){ list.remove(j); } } }
		 */

		for (int i = list.size() - 1; i >= 0; i--) {

			if (list.get(i) instanceof BooleanConstant) {

				if (((BooleanConstant) list.get(i)).get_value()) {
					return FormulaFactory.mkConst(true);
				}
				list.remove(i);
			}
		}

		
		// put all Literals together (and check for trivial
		// tautologies/contradictions like a and a /a and !a
		for (int i = list.size() - 1; i >= 0; i--) {
			if (list.get(i) instanceof Literal) {
				helper.add(list.get(i));
				list.remove(i);

			}
		}
		for (int i = 0; i < helper.size(); i++) {

			for (int j = i + 1; j < helper.size(); j++) {
				if (((Literal) helper.get(i)).atom.equals(((Literal) helper.get(j)).atom)) {
					if (((Literal) helper.get(i)).negated == (((Literal) helper.get(j)).negated)) {
						helper.remove(j);
					} else {
						return FormulaFactory.mkConst(true);
					}
				}
			}
		}
		list.addAll(helper);
		// System.out.println("Children: "+list.toString());
		if (list.size() == 0) {
			return FormulaFactory.mkConst(false);
		} else if (list.size() == 1) {
			return list.get(0);
		} else {
			// compare list and children and only make a new dis-
			// junction if both are different (circumventing a stackoverflow)
			if (list.size() != children.size()) {
				return FormulaFactory.mkOr(list);
			}

			// Therefore list has to be ordered
			for (int i = 0; i < list.size(); i++) {
				for (int j = i + 1; j < list.size(); j++) {
					if (list.get(i).get_id() > list.get(j).get_id()) {
						Formula swap = list.get(i);
						list.set(i, list.get(j));
						list.set(j, swap);
					}
				}

			}

			for (int i = 0; i < list.size(); i++) {
				if (list.get(i).get_id() != children.get(i).get_id()) {
					return FormulaFactory.mkOr(list);
				}
			}

			return this;
		}

	}

	private ArrayList<Formula> getAllChildrenOfDisjunction() {
		ArrayList<Formula> al = new ArrayList<Formula>();
		for (Formula child : children) {
			if (child instanceof Disjunction) {
				al.addAll(((Disjunction) child).getAllChildrenOfDisjunction());
			} else {
				al.add(child);
			}
		}

		// sort them according to unique_id:
		Formula swap;
		for (int i = 0; i < al.size(); i++) {
			for (int j = 0; j < al.size(); j++) {
				if (al.get(i).unique_id > al.get(j).unique_id) {
					swap = al.get(i);
					al.set(i, al.get(j));
					al.set(j, swap);
				}
			}
		}

		return al;

	}

	private int init_hash() {
		int offset = 31583;
		int hash = 1;
		for (Formula child : children) {
			hash %= offset;
			hash = hash * (child.hashCode() % 34631);
		}
		return (hash + 2503) % 999983;
	}

}
