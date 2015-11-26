/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rabinizer.automata;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rabinizer.bdd.MyBDD;
import rabinizer.bdd.Valuation;
import rabinizer.bdd.ValuationSet;
import rabinizer.formulas.Formula;

/**
 *
 * @author jkretinsky
 */
public class DTGRA extends Product implements AccAutomatonInterface {

	AccTGR acc;

	public DTGRA(FormulaAutomaton master, Map<Formula, RabinSlave> slaves) {
		super(master, slaves);
	}

	public DTGRA(DTGRARaw raw) {
		super(raw.automaton);
		if (raw.accTGR != null) { // for computing the state space only (with no
									// acc. condition)
			this.acc = new AccTGR(raw.accTGR);
		}
	}

	@Override
	protected String accName() {
		String result = "acc-name: generalized-Rabin " + acc.size();
		for (int i = 0; i < acc.size(); i++) {
			result += " " + acc.get(i).right.size();
		}
		return result + "\n";
	}

	@Override
	protected String accTypeNumerical() {
		if (acc.size() == 0) {
			return "0 f";
		}
		String result = "";
		int sum = 0;
		for (int i = 0; i < acc.size(); i++) {
			result += i == 0 ? "" : " | ";
			result += "Fin(" + sum + ")";
			sum++;
			List<TranSet<ProductState>> right = acc.get(i).right;
			for (int j = 0; j < right.size(); j++) {
				right.get(j);
				result += "&Inf(" + sum + ")";
				sum++;
			}
		}
		return sum + " " + result;
	}

	@Override
	protected String stateAcc(ProductState s) {
		return "";
	}

	@Override
	protected String outTransToHOA(ProductState s) {
		String result = "";
		Set<Set<ValuationSet>> productVs = new HashSet<Set<ValuationSet>>();
		productVs.add(transitions.get(s).keySet());
		Set<ValuationSet> vSets;
		for (GRabinPairT rp : acc) {
			vSets = new HashSet<ValuationSet>();
			if (rp.left.containsKey(s)) {
				vSets.add(rp.left.get(s));
				vSets.add(rp.left.get(s).complement());
			}
			productVs.add(vSets);
			for (TranSet<ProductState> ts : rp.right) {
				vSets = new HashSet<ValuationSet>();
				if (ts.containsKey(s)) {
					vSets.add(ts.get(s));
					vSets.add(ts.get(s).complement());
				}
				productVs.add(vSets);
			}
		}
		vSets = new HashSet<ValuationSet>();
		productVs.remove(vSets);
		Set<ValuationSet> edges = generatePartitioning(productVs);
		for (ValuationSet vsSep : edges) {
			Valuation v = vsSep.pickAny();
			result += "[" + (new MyBDD(vsSep.toBdd(), true)).BDDtoNumericString() + "] "
					+ statesToNumbers.get(succ(s, v)) + " {" + acc.accSets(s, v) + "}\n";
		}
		return result;
	}

	@Override
	public String acc() {
		return acc.toString();
	}

	public int pairNumber() {
		return acc.size();
	}

}
