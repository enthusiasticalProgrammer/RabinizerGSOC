/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rabinizer.automata;

import rabinizer.ltl.Formula;
import rabinizer.ltl.ValuationSet;
import rabinizer.ltl.ValuationSetFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author jkretinsky
 */
public class Product extends Automaton<ProductState> {

    public final FormulaAutomaton master;
    public final Map<Formula, RabinSlave> slaves;
    public final Set<Formula> allSlaves;

    public Product(FormulaAutomaton master, Map<Formula, RabinSlave> slaves, ValuationSetFactory<String> factory) {
        super(factory);
        this.master = master;
        this.slaves = slaves;
        allSlaves = slaves.keySet();/*new HashSet();
         for (Formula slaveFormula : slaves.keySet()) {
         allSlaves.add(slaveFormula);
         }*/
    }

    public Product(Product a) {
        super(a);
        master = a.master;
        slaves = a.slaves;
        allSlaves = a.allSlaves;
    }

    @Override
    protected ProductState generateInitialState() {
        ProductState init = new ProductState(master.initialState);
        for (Formula slave : relevantSlaves(master.initialState)) {
            init.put(slave, slaves.get(slave).initialState);
        }
        return init;
    }

    protected Set<Formula> relevantSlaves(FormulaAutomatonState masterState) {
        return masterState.getFormula().relevantGFormulas(allSlaves);
    }

    @Override   //TODO compute labels after construction would be faster
    protected ProductState generateSuccState(ProductState s, ValuationSet vs) {
        Set<String> val = vs.pickAny();
        ProductState succ = new ProductState(master.succ(s.masterState, val));
        //String label = master.stateLabels.get(master.succ(s.masterState, val)) + "::";
        for (Formula slave : relevantSlaves(succ.masterState)) {
            if (s.containsKey(slave)) {
                succ.put(slave, slaves.get(slave).succ(s.get(slave), val));
            } else {
                succ.put(slave, slaves.get(slave).initialState);
            }
            //label += slaves.get(slave).stateLabels.get(succ.get(slave));
        }
        return succ;
    }

    @Override
    protected Set<ValuationSet> generateSuccTransitions(ProductState s) {
        Set<Set<ValuationSet>> product = new HashSet<>();
        product.add(master.transitions.row(s.masterState).keySet());
        for (Map.Entry<Formula, RankingState> formulaRankingStateEntry : s.entrySet()) {
            product.add(slaves.get(formulaRankingStateEntry.getKey()).transitions.row(formulaRankingStateEntry.getValue()).keySet());
        }
        return generatePartitioning(product);
    }

    Set<ValuationSet> generateSuccTransitionsReflectingSinks(ProductState s) {
        Set<Set<ValuationSet>> product = new HashSet<>();
        product.add(master.transitions.row(s.masterState).keySet());
        for (Formula slaveFormula : s.keySet()) {
            FormulaAutomaton m = slaves.get(slaveFormula).mojmir;
            for (FormulaAutomatonState fs : m.states) {
                product.add(m.transitions.row(fs).keySet());
            }
        }
        product.removeIf(Set::isEmpty); // removing empty trans due to sinks
        return generatePartitioning(product);
    }

}
