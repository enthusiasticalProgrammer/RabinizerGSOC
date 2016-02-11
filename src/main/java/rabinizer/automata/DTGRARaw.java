package rabinizer.automata;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import rabinizer.automata.Product.ProductState;
import rabinizer.exec.Main;
import rabinizer.ltl.equivalence.EquivalenceClassFactory;
import rabinizer.ltl.Formula;
import rabinizer.ltl.GOperator;
import rabinizer.collections.valuationset.ValuationSetFactory;

/**
 * @author jkretinsky
 */
public class DTGRARaw {

    final EquivalenceClassFactory equivalenceClassFactory;
    final ValuationSetFactory valuationSetFactory;
    public Product automaton;
    public AccTGRRaw<ProductState> accTGR;
    AccLocal accLocal;

    public DTGRARaw(Formula phi, EquivalenceClassFactory equivalenceClassFactory,
            ValuationSetFactory valuationSetFactory, Collection<Optimisation> opts) {
        this.valuationSetFactory = valuationSetFactory;
        this.equivalenceClassFactory = equivalenceClassFactory;

        // phi assumed in NNF
        Main.verboseln("========================================");
        Main.nonsilent("Generating primaryAutomaton");
        Master master;
        boolean mergingEnabled = true;

        master = new Master(phi, equivalenceClassFactory, valuationSetFactory, opts, mergingEnabled);
        master.generate();

        Main.verboseln("========================================");
        Main.nonsilent("Generating Mojmir & Rabin secondaryAutomata");
        Set<GOperator> gSubformulas = phi.gSubformulas();
        Map<GOperator, RabinSlave> slaves = new HashMap<>();
        for (GOperator f : gSubformulas) {
            MojmirSlave mSlave;

            mSlave = new MojmirSlave(f, equivalenceClassFactory, valuationSetFactory, opts);
            mSlave.generate();

            if (opts.contains(Optimisation.SINKS)) { // selfloop-only states
                // keep no tokens
                mSlave.removeSinks();
            }
            RabinSlave rSlave = new RabinSlave(mSlave, valuationSetFactory);
            rSlave.generate();
            if (opts.contains(Optimisation.OPTIMISE_INITIAL_STATE)) { // remove
                // transient
                // part
                rSlave.optimizeInitialState();
            }
            slaves.put(f, rSlave);
        }
        Main.verboseln("========================================");
        Main.nonsilent("Generating product");

        automaton = new Product(master, slaves, valuationSetFactory, opts);

        automaton.generate();
        if (opts.contains(Optimisation.COMPUTE_ACC_CONDITION)) {
            Main.verboseln("========================================");
            Main.nonsilent("Generating local acceptance conditions");
            if (opts.contains(Optimisation.EAGER) && !opts.contains(Optimisation.NOT_ISABELLE_ACC)) {
                accLocal = new AccLocal(automaton, valuationSetFactory, equivalenceClassFactory, opts);
            } else {
                accLocal = new AccLocalFolded(automaton, valuationSetFactory, equivalenceClassFactory, opts);
            }
            Main.verboseln("========================================");
            Main.nonsilent("Generating global acceptance condition");
            accTGR = new AccTGRRaw<>(accLocal, valuationSetFactory, equivalenceClassFactory);
            if (opts.contains(Optimisation.EMPTINESS_CHECK)) {
                checkIfEmptyAndRemoveEmptySCCs();
            } else if (opts.contains(Optimisation.COMPLETE)) {
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
            for (GRabinPairRaw<ProductState> rabPair : accTGR) {
                rabPair.left.put(automaton.trapState, valuationSetFactory.createUniverseValuationSet());
            }
        }

    }

}
