package rabinizer.automata;

import rabinizer.exec.Main;
import rabinizer.collections.Tuple;
import rabinizer.collections.valuationset.ValuationSet;
import rabinizer.collections.valuationset.ValuationSetFactory;

import java.util.Map;
import java.util.Set;

public class RabinPair<S> extends Tuple<TranSet<S>, TranSet<S>> {

    public RabinPair(TranSet<S> l, TranSet<S> r) {
        super(l, r);
    }

    public RabinPair(RabinPair<S> rp) {
        super(rp.left, rp.right);
    }

    public static RabinPair<Product.ProductState> createRabinPair(RabinSlave slave, Set<MojmirSlave.State> finalStates, int rank, Product product,
                                                                     ValuationSetFactory valuationSetFactory) {
        return new RabinPair<>(RabinPair.fromSlave(slave, finalStates, rank, product, valuationSetFactory));
    }

    private static RabinPair<Product.ProductState> fromSlave(RabinSlave slave, Set<MojmirSlave.State> finalStates, int rank,
                                                             Product product, ValuationSetFactory valuationSetFactory) {

        // Set fail
        // Mojmir
        TranSet<MojmirSlave.State> failM = new TranSet<>(valuationSetFactory);
        for (MojmirSlave.State fs : slave.mojmir.states) {
            // if (!slave.mojmir.sinks.contains(fs)) {
            for (Map.Entry<ValuationSet, MojmirSlave.State> vsfs : slave.mojmir.transitions.row(fs).entrySet()) {
                if (slave.mojmir.isSink(vsfs.getValue()) && !finalStates.contains(vsfs.getValue())) {
                    failM.add(fs, vsfs.getKey());
                }
            }
            // }
        }
        // Product
        TranSet<Product.ProductState> failP = new TranSet<>(valuationSetFactory);
        for (Product.ProductState ps : product.states) {
            RabinSlave.State rs = ps.getSecondaryState(slave.mojmir.label);
            if (rs != null) { // relevant slave
                for (MojmirSlave.State fs : rs.keySet()) {
                    if (failM.containsKey(fs)) {
                        failP.add(ps, failM.get(fs));
                    }
                }
            }
        }

        // Set succeed(pi)
        // Mojmir
        TranSet<MojmirSlave.State> succeedM = new TranSet<>(valuationSetFactory);
        if (finalStates.contains(slave.mojmir.getInitialState())) {
            for (MojmirSlave.State fs : slave.mojmir.states) {
                for (Map.Entry<ValuationSet, MojmirSlave.State> vsfs : slave.mojmir.transitions.row(fs)
                        .entrySet()) {
                    succeedM.add(fs, vsfs.getKey());
                }
            }
        } else {
            for (MojmirSlave.State fs : slave.mojmir.states) {
                if (!finalStates.contains(fs)) {
                    for (Map.Entry<ValuationSet, MojmirSlave.State> vsfs : slave.mojmir.transitions.row(fs)
                            .entrySet()) {
                        if (finalStates.contains(vsfs.getValue())) {
                            succeedM.add(fs, vsfs.getKey());
                        }
                    }
                }
            }
        }
        // Product
        TranSet<Product.ProductState> succeedP = new TranSet<>(valuationSetFactory);
        for (Product.ProductState ps : product.states) {
            RabinSlave.State rs = ps.getSecondaryState(slave.mojmir.label);
            if (rs != null) { // relevant slave
                for (Map.Entry<MojmirSlave.State, Integer> stateIntegerEntry : rs.entrySet()) {
                    if (succeedM.containsKey(stateIntegerEntry.getKey()) && stateIntegerEntry.getValue() == rank) {
                        succeedP.add(ps, succeedM.get(stateIntegerEntry.getKey()));
                    }
                }
            }
        }

        // Set buy(pi)
        // Rabin
        TranSet<RabinSlave.State> buyR = new TranSet<>(valuationSetFactory);
        for (RabinSlave.State rs : slave.states) {
            for (Map.Entry<MojmirSlave.State, Integer> stateIntegerEntry : rs.entrySet()) {
                if (stateIntegerEntry.getValue() < rank) {
                    for (MojmirSlave.State fs2 : rs.keySet()) {
                        for (MojmirSlave.State succ : slave.mojmir.states) {
                            ValuationSet vs1, vs2;
                            if (!finalStates.contains(succ) && (vs1 = slave.mojmir.edgeBetween.get(stateIntegerEntry.getKey(), succ)) != null
                                    && (vs2 = slave.mojmir.edgeBetween.get(fs2, succ)) != null) {
                                if (!stateIntegerEntry.getKey().equals(fs2)) {
                                    ValuationSet vs1copy = vs1.clone();
                                    vs1copy.retainAll(vs2);
                                    buyR.add(rs, vs1copy);
                                } else if (succ.equals(slave.mojmir.getInitialState())) {
                                    buyR.add(rs, vs1);
                                }

                            }
                        }
                    }
                }
            }
        }
        // Product
        TranSet<Product.ProductState> buyP = new TranSet<>(valuationSetFactory);
        for (Product.ProductState ps : product.states) {
            RabinSlave.State rs = ps.getSecondaryState(slave.mojmir.label);
            if (rs != null) { // relevant slave
                if (buyR.containsKey(rs)) {
                    buyP.add(ps, buyR.get(rs));
                }
            }
        }

        Main.verboseln("\tAn acceptance pair for slave " + slave.mojmir.label + ":\n" + failP + buyP + succeedP);
        failP.addAll(buyP);
        return new RabinPair<>(failP, succeedP);
    }

    @Override
    public String toString() {
        return "Fin:\n" + (left == null ? "trivial" : left) + "\nInf:\n"
                + (right == null ? "trivial" : right);
    }

}