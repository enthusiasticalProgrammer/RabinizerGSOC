/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rabinizer.automata;

import rabinizer.formulas.Formula;

import java.util.Map;
import java.util.Set;

/**
 * @author jkretinsky
 */
public class ProductAllSlaves extends Product {

    public ProductAllSlaves(FormulaAutomaton master, Map<Formula, RabinSlave> slaves) {
        super(master, slaves);
    }

    @Override
    protected Set<Formula> relevantSlaves(FormulaState masterState) {
        return allSlaves;
    }

}
