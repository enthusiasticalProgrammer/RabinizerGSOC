package rabinizer.automata;

import rabinizer.exec.Main;
import rabinizer.ltl.EquivalenceClassFactory;
import rabinizer.ltl.Formula;
import rabinizer.ltl.ValuationSet;
import rabinizer.ltl.ValuationSetFactory;
import rabinizer.ltl.GOperator;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Table.Cell;

/**
 * @author jkretinsky
 */
public class DTGRARaw {

    public Product automaton;
    public AccTGRRaw accTGR;
    AccLocal accLocal;

    final EquivalenceClassFactory equivalenceClassFactory;
    final ValuationSetFactory<String> valuationSetFactory;

    public DTGRARaw(Formula phi, boolean computeAcc, boolean unfoldedOn, boolean sinksOn,
            boolean optimizeInitialStatesOn, boolean relevantSlavesOnlyOn, boolean slowerIsabelleAccForUnfolded,
            EquivalenceClassFactory equivalenceClassFactory, ValuationSetFactory<String> valuationSetFactory) {
        this.valuationSetFactory = valuationSetFactory;
        this.equivalenceClassFactory = equivalenceClassFactory;

        // phi assumed in NNF
        Main.verboseln("========================================");
        Main.nonsilent("Generating primaryAutomaton");
        FormulaAutomaton master;
        if (unfoldedOn) { // unfold upon arrival to state
            master = new Master(phi, equivalenceClassFactory, valuationSetFactory);
        } else {
            master = new MasterFolded(phi, equivalenceClassFactory, valuationSetFactory);
        }
        master.generate();
        Main.verboseln("========================================");
        Main.nonsilent("Generating Mojmir & Rabin secondaryAutomata");
        Set<GOperator> gSubformulas = phi.gSubformulas();
        Map<GOperator, RabinSlave> slaves = new HashMap<>();
        for (GOperator f : gSubformulas) {
            FormulaAutomaton mSlave;
            if (unfoldedOn) { // unfold upon arrival to state
                mSlave = new MojmirSlave(f, equivalenceClassFactory, valuationSetFactory);
            } else {
                mSlave = new MojmirSlaveFolded(f, equivalenceClassFactory, valuationSetFactory);
            }
            mSlave.generate();
            if (sinksOn) { // selfloop-only states keep no tokens
                mSlave.removeSinks();
            }
            RabinSlave rSlave = new RabinSlave(mSlave, valuationSetFactory);
            rSlave.generate();
            if (optimizeInitialStatesOn) { // remove transient part
                rSlave.optimizeInitialState();
            }
            slaves.put(f, rSlave);
        }
        Main.verboseln("========================================");
        Main.nonsilent("Generating product");
        if (relevantSlavesOnlyOn) { // relevant secondaryAutomata dynamically
                                    // computed from primaryAutomaton formula
                                    // master formula
            automaton = new Product(master, slaves, valuationSetFactory);
        } else { // all secondaryAutomata monitor
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

    /**
     * Side effect: empty sink-SCCs get deleted, acceptance condition gets
     * reduced when possible
     * 
     * @return true if automaton together witch acceptance condition is empty
     */
    public boolean checkIfEmpty(ValuationSetFactory<String> val) {
        return EmptinessCheck.<ProductState> checkEmptiness((Automaton<ProductState>) automaton, accTGR, val);
    }

}
