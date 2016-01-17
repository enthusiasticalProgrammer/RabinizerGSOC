package rabinizer.automata;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Table;

import rabinizer.automata.Product.ProductState;
import rabinizer.exec.Main;
import rabinizer.ltl.EquivalenceClassFactory;
import rabinizer.ltl.Formula;
import rabinizer.ltl.GOperator;
import rabinizer.ltl.ValuationSet;
import rabinizer.ltl.ValuationSetFactory;

/**
 * @author jkretinsky
 */
public class DTGRARaw {

    final EquivalenceClassFactory equivalenceClassFactory;
    final ValuationSetFactory valuationSetFactory;
    public Product automaton;
    public AccTGRRaw<ProductState> accTGR;
    AccLocal accLocal;

    public DTGRARaw(Formula phi, boolean computeAcc, boolean unfoldedOn, boolean sinksOn,
            boolean optimizeInitialStatesOn, boolean relevantSlavesOnlyOn, boolean slowerIsabelleAccForUnfolded,
            EquivalenceClassFactory equivalenceClassFactory, ValuationSetFactory valuationSetFactory,
            boolean complete, boolean emptinessCheck) {
        this.valuationSetFactory = valuationSetFactory;
        this.equivalenceClassFactory = equivalenceClassFactory;

        // phi assumed in NNF
        Main.verboseln("========================================");
        Main.nonsilent("Generating primaryAutomaton");
        Master master;
        boolean mergingEnabled = true;
        if (unfoldedOn) { // unfold upon arrival to state
            master = new Master(phi, equivalenceClassFactory, valuationSetFactory, EnumSet.of(Optimisation.EAGER),
                    mergingEnabled);
        } else {
            master = new Master(phi, equivalenceClassFactory, valuationSetFactory, Collections.emptySet(),
                    mergingEnabled);
        }
        master.generate();
        Main.verboseln("========================================");
        Main.nonsilent("Generating Mojmir & Rabin secondaryAutomata");
        Set<GOperator> gSubformulas = phi.gSubformulas();
        Map<GOperator, RabinSlave> slaves = new HashMap<>();
        for (GOperator f : gSubformulas) {
            MojmirSlave mSlave;
            if (unfoldedOn) { // unfold upon arrival to state
                mSlave = new MojmirSlave(f, equivalenceClassFactory, valuationSetFactory,
                        EnumSet.of(Optimisation.EAGER));
            } else {
                mSlave = new MojmirSlave(f, equivalenceClassFactory, valuationSetFactory, Collections.emptySet());
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
            automaton = new Product(master, slaves, valuationSetFactory, Collections.emptySet());
        } else { // all secondaryAutomata monitor
            automaton = new Product(master, slaves, valuationSetFactory, EnumSet.of(Optimisation.ALL_SLAVES));
        }
        automaton.generate();
        if (computeAcc) {
            Main.verboseln("========================================");
            Main.nonsilent("Generating local acceptance conditions");
            if (unfoldedOn && slowerIsabelleAccForUnfolded) {
                accLocal = new AccLocal(automaton, valuationSetFactory, equivalenceClassFactory);
            } else {
                accLocal = new AccLocalFolded(automaton, valuationSetFactory, equivalenceClassFactory);
            }
            Main.verboseln("========================================");
            Main.nonsilent("Generating global acceptance condition");
            accTGR = new AccTGRRaw<>(accLocal, valuationSetFactory, equivalenceClassFactory);
            if (emptinessCheck) {
                checkIfEmptyAndRemoveEmptySCCs();
            } else if (complete) {
                completeAutomaton();
            }
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
    public boolean checkIfEmptyAndRemoveEmptySCCs() {
        boolean result = EmptinessCheck.checkEmptiness(automaton, accTGR);
        this.completeAutomaton();
        return result;
    }

    public void completeAutomaton() {
        automaton.makeComplete();

        if (automaton.states.contains(automaton.trapState)) {
            for (Table.Cell<ProductState, ValuationSet, ProductState> entry : automaton.transitions.cellSet()) {
                if (entry.getRowKey().equals(automaton.trapState)) {
                    for (GRabinPairRaw<ProductState> rabPair : accTGR) {
                        Map<ProductState, ValuationSet> finTrans = rabPair.left;
                        if (finTrans.get(entry.getRowKey()) != null) {
                            ValuationSet newVal = finTrans.get(entry.getRowKey());
                            newVal.addAll(entry.getColumnKey());
                            finTrans.put(entry.getRowKey(), newVal);
                        } else {
                            finTrans.put(entry.getRowKey(), entry.getColumnKey());
                        }
                    }
                }
            }
        }

    }

}
