/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rabinizer.automata;

import rabinizer.exec.Main;
import rabinizer.ltl.EquivalenceClassFactory;
import rabinizer.ltl.Formula;
import rabinizer.ltl.GOperator;
import rabinizer.ltl.ValuationSetFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author jkretinsky
 */
public class DTGRARaw /*implements AccAutomatonInterface*/ {

    public Product automaton;
    public AccTGRRaw accTGR;
    AccLocal accLocal;

    final EquivalenceClassFactory equivalenceClassFactory;
    final ValuationSetFactory<String> valuationSetFactory;

    /**
     * @param phi
     * @param computeAcc
     * @param unfoldedOn
     * @param sinksOn
     * @param optimizeInitialStatesOn
     * @param relevantSlavesOnlyOn
     * @param slowerIsabelleAccForUnfolded
     */
    public DTGRARaw(Formula phi, boolean computeAcc, boolean unfoldedOn, boolean sinksOn, boolean optimizeInitialStatesOn, boolean relevantSlavesOnlyOn, boolean slowerIsabelleAccForUnfolded, EquivalenceClassFactory equivalenceClassFactory, ValuationSetFactory<String> valuationSetFactory) {
        this.valuationSetFactory = valuationSetFactory;
        this.equivalenceClassFactory = equivalenceClassFactory;

        // phi assumed in NNF
        Main.verboseln("========================================");
        Main.nonsilent("Generating master");
        FormulaAutomaton master;
        if (unfoldedOn) { // unfold upon arrival to state
            master = new Master(phi, equivalenceClassFactory, valuationSetFactory);
        } else {
            master = new MasterFolded(phi, equivalenceClassFactory, valuationSetFactory);
        }
        master.generate();
        Main.verboseln("========================================");
        Main.nonsilent("Generating Mojmir & Rabin slaves");
        Set<Formula> gSubformulas = phi.gSubformulas().stream().map(c -> c.getOperand()).collect(Collectors.toSet()); // without outer G
        Map<Formula, RabinSlave> slaves = new HashMap<>();
        for (Formula f : gSubformulas) {
            FormulaAutomaton mSlave;
            if (unfoldedOn) {  // unfold upon arrival to state
                mSlave = new MojmirSlave(f, equivalenceClassFactory, valuationSetFactory);
            } else {
                mSlave = new MojmirSlaveFolded(f, equivalenceClassFactory, valuationSetFactory);
            }
            mSlave.generate();
            if (sinksOn) {  // selfloop-only states keep no tokens
                mSlave.useSinks();
            }
            RabinSlave rSlave = new RabinSlave(mSlave, valuationSetFactory);
            rSlave.generate();
            if (optimizeInitialStatesOn) {  // remove transient part
                rSlave.optimizeInitialState();
            }
            slaves.put(f, rSlave);
        }
        Main.verboseln("========================================");
        Main.nonsilent("Generating product");
        if (relevantSlavesOnlyOn) {  // relevant slaves dynamically computed from master formula
            automaton = new Product(master, slaves, valuationSetFactory);
        } else {  // all slaves monitor
            automaton = new ProductAllSlaves(master, slaves, valuationSetFactory);
        }
        automaton.generate();
        if (computeAcc) {
            Main.verboseln("========================================");
            Main.nonsilent("Generating local acceptance conditions");
            if (unfoldedOn & slowerIsabelleAccForUnfolded) {
                accLocal = new AccLocal(automaton, valuationSetFactory, equivalenceClassFactory);
            } else {
                accLocal = new AccLocalFolded(automaton, valuationSetFactory, equivalenceClassFactory);
            }
            Main.verboseln("========================================");
            Main.nonsilent("Generating global acceptance condition");
            accTGR = new AccTGRRaw(accLocal, valuationSetFactory, equivalenceClassFactory);
            Main.nonsilent("Generating optimized acceptance condition");
            accTGR.removeRedundancy();
            Main.verboseln("========================================");
        }
    }


}
