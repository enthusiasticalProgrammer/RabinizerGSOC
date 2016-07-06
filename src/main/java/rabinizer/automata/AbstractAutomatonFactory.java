package rabinizer.automata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jhoafparser.consumer.HOAConsumerPrint;
import ltl.Formula;
import ltl.ModalOperator;
import ltl.equivalence.EquivalenceClassFactory;
import omega_automaton.collections.TranSet;
import omega_automaton.collections.Tuple;
import omega_automaton.collections.valuationset.ValuationSetFactory;
import rabinizer.exec.Main;
import rabinizer.frequencyLTL.SlaveSubformulaVisitor;
import rabinizer.automata.Optimisation;
import rabinizer.automata.Product.ProductState;

/**
 * This class contains the behaviour, which belongs to both constructions:
 * rabinizer and controller synthesis for MDPs and Frequency LTL \ GU.
 *
 * @param <T>
 *            type of self product slaves
 * @param <P>
 *            type of overall product
 */
public abstract class AbstractAutomatonFactory<T extends AbstractSelfProductSlave<?>, P extends Product> {
    protected final Formula phi;
    protected final EquivalenceClassFactory equivalenceClassFactory;
    protected final ValuationSetFactory valuationSetFactory;
    protected final Collection<Optimisation> opts;
    protected P product;

    protected AbstractAutomatonFactory(Formula phi, EquivalenceClassFactory equivalenceClassFactory, ValuationSetFactory valuationSetFactory, Collection<Optimisation> opts) {
        this.phi = phi;
        this.equivalenceClassFactory = equivalenceClassFactory;
        this.valuationSetFactory = valuationSetFactory;
        this.opts = opts;
    }

    public final P constructAutomaton() {
        Master master = constructMaster();

        Map<ModalOperator, T> slaves = constructSlaves();

        Main.nonsilent("========================================");
        Main.nonsilent("Generating product\n");

        product = obtainProduct(master, slaves);
        product.generate();

        if (!Main.silent) {
            HOAConsumerPrint hoa = new HOAConsumerPrint(System.out);
            product.toHOA(hoa, null);
        }

        if (opts.contains(Optimisation.COMPUTE_ACC_CONDITION)) {
            Main.nonsilent("========================================");
            Main.nonsilent("Generating acceptance condition\n");

            constructAcceptance();

            Main.nonsilent("========================================");
            Main.nonsilent("Remove some redundancy of Acceptance Condition\n");

            removeRedundancy();

            Main.nonsilent("========================================");
            Main.nonsilent("Doing post-processing optimisations\n");

            doPostProcessingOptimisations();
        }
        return product;

    }

    protected final void removeRedundancy() {
        List<TranSet<ProductState<?>>> copy;
        Collection<Tuple<TranSet<ProductState<?>>, List<TranSet<ProductState<?>>>>> toRemove = new HashSet<>();

        product.getAcceptance().acceptanceCondition.stream().filter(pair -> product.containsAllTransitions(pair.left)).forEach(s -> toRemove.add(s));
        product.getAcceptance().deleteTheFollowingAcceptanceConditions(toRemove);
        toRemove.clear();

        product.getAcceptance().acceptanceCondition.forEach(pair -> pair.right.forEach(inf -> inf.removeAll(pair.left)));

        product.getAcceptance().acceptanceCondition.stream().filter(pair -> pair.right.stream().anyMatch(TranSet::isEmpty)).forEach(s -> toRemove.add(s));
        product.getAcceptance().deleteTheFollowingAcceptanceConditions(toRemove);
        toRemove.clear();

        product.getAcceptance().acceptanceCondition.forEach(pair -> pair.right.removeIf(i -> product.containsAllTransitions(i.union(pair.left))));

        Collection<Tuple<TranSet<ProductState<?>>, List<TranSet<ProductState<?>>>>> temp = new ArrayList<>();
        for (Tuple<TranSet<ProductState<?>>, List<TranSet<ProductState<?>>>> pair : product.getAcceptance().acceptanceCondition) {
            copy = new ArrayList<>(pair.right);
            for (TranSet<ProductState<?>> i : pair.right) {
                for (TranSet<ProductState<?>> j : pair.right) {
                    if (!j.equals(i) && i.containsAll(j)) {
                        copy.remove(i);
                        break;
                    }
                }
            }
            temp.add(new Tuple<>(pair.left, copy));
        }
        product.getAcceptance().acceptanceCondition.clear();
        product.getAcceptance().acceptanceCondition.addAll(temp);

        for (Tuple<TranSet<ProductState<?>>, List<TranSet<ProductState<?>>>> pair1 : product.getAcceptance().acceptanceCondition) {
            for (Tuple<TranSet<ProductState<?>>, List<TranSet<ProductState<?>>>> pair2 : product.getAcceptance().acceptanceCondition) {
                if (pair1.equals(pair2)) {
                    continue;
                }

                if (product.getAcceptance().implies(pair2, pair1) && !toRemove.contains(pair1)) {
                    toRemove.add(pair2);
                    break;
                }
            }
        }

        product.getAcceptance().deleteTheFollowingAcceptanceConditions(toRemove);
    }


    protected abstract P obtainProduct(Master master, Map<ModalOperator, T> slaves);

    protected abstract void doPostProcessingOptimisations();

    protected abstract void constructAcceptance();

    final Master constructMaster() {
        Main.nonsilent("========================================");
        Main.nonsilent("Generating primaryAutomaton:\n");
        Master master;

        if (opts.contains(Optimisation.SLAVE_SUSPENSION)) {
            master = new SuspendedMaster(phi, equivalenceClassFactory, valuationSetFactory, opts);
        } else {
            master = new Master(phi, equivalenceClassFactory, valuationSetFactory, opts);
        }
        master.generate();
        if (!Main.silent) {
            HOAConsumerPrint hoa = new HOAConsumerPrint(System.out);
            master.toHOA(hoa, null);
        }
        return master;
    }

    private final Map<ModalOperator, T> constructSlaves() {
        Set<ModalOperator> gSubformulas = phi.accept(new SlaveSubformulaVisitor());
        Map<ModalOperator, T> slaves = new HashMap<>();

        for (ModalOperator f : gSubformulas) {

            MojmirSlave mSlave = new MojmirSlave(f, equivalenceClassFactory, valuationSetFactory, opts);
            mSlave.generate();

            if (Main.verbose) {
                HOAConsumerPrint hoa = new HOAConsumerPrint(System.out);
                Main.verboseln("Mojmir Slave: ");
                mSlave.toHOA(hoa, null);
            }

            T rSlave = obtainSelfProductSlave(mSlave);
            rSlave.generate();

            optimizeInitialStateOfSelfProductSlave(rSlave);

            slaves.put(f, rSlave);

            if (Main.verbose) {
                HOAConsumerPrint hoa = new HOAConsumerPrint(System.out);
                Main.verboseln("\nRabin Slave: ");
                rSlave.toHOA(hoa, null);
            }
        }
        return slaves;
    }

    protected abstract void optimizeInitialStateOfSelfProductSlave(T rSlave);

    protected abstract T obtainSelfProductSlave(MojmirSlave mSlave);
}
