/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rabinizer.automata;

import rabinizer.ltl.Formula;
import rabinizer.ltl.ValuationSetFactory;

import java.util.Map;
import java.util.Set;

/**
 * @author jkretinsky
 */
public class ProductAllSlaves extends Product {

    public ProductAllSlaves(FormulaAutomaton master, Map<Formula, RabinSlave> slaves, ValuationSetFactory<String> factory) {
        super(master, slaves, factory);
    }

    @Override
    protected Set<Formula> relevantSlaves(FormulaState masterState) {
        return allSlaves;
    }

}
