package rabinizer.automata;

import rabinizer.exec.Main;
import rabinizer.exec.Tuple;
import rabinizer.ltl.ValuationSet;
import rabinizer.ltl.ValuationSetFactory;

import java.util.Map;

/**
 * @param <State>
 * @author jkretinsky
 */
public class RabinPair<State> extends Tuple<TranSet<State>, TranSet<State>> {

    public RabinPair(TranSet<State> l, TranSet<State> r) {
        super(l, r);
    }

    public RabinPair(RabinPair<State> rp) {
        super(rp.getLeft(), rp.getRight());
    }

    public RabinPair(RabinSlave slave, Map<FormulaAutomatonState, Boolean> finalStates, int rank, Product product,
            ValuationSetFactory<String> valuationSetFactory) {
        this(RabinPair.fromSlave(slave, finalStates, rank, product, valuationSetFactory));
    }

    private static RabinPair fromSlave(RabinSlave slave, Map<FormulaAutomatonState, Boolean> finalStates, int rank,
            Product product, ValuationSetFactory<String> valuationSetFactory) {

        // Set fail
        // Mojmir
        TranSet<FormulaAutomatonState> failM = new TranSet<>(valuationSetFactory);
        for (FormulaAutomatonState fs : slave.mojmir.states) {
            // if (!slave.mojmir.sinks.contains(fs)) {
            for (Map.Entry<ValuationSet, FormulaAutomatonState> vsfs : slave.mojmir.transitions.row(fs).entrySet()) {
                if (slave.mojmir.sinks.contains(vsfs.getValue()) && !finalStates.get(vsfs.getValue())) {
                    failM.add(fs, vsfs.getKey());
                }
            }
            // }
        }
        // Product
        TranSet<ProductState> failP = new TranSet<>(valuationSetFactory);
        for (ProductState ps : product.states) {
            RankingState rs = ps.getSecondaryState(slave.mojmir.getFormula());
            if (rs != null) { // relevant slave
                for (FormulaAutomatonState fs : rs.keySet()) {
                    if (failM.containsKey(fs)) {
                        failP.add(ps, failM.get(fs));
                    }
                }
            }
        }

        // Set succeed(pi)
        // Mojmir
        TranSet<FormulaAutomatonState> succeedM = new TranSet<>(valuationSetFactory);
        if (finalStates.get(slave.mojmir.initialState)) {
            for (FormulaAutomatonState fs : slave.mojmir.states) {
                for (Map.Entry<ValuationSet, FormulaAutomatonState> vsfs : slave.mojmir.transitions.row(fs)
                        .entrySet()) {
                    succeedM.add(fs, vsfs.getKey());
                }
            }
        } else {
            for (FormulaAutomatonState fs : slave.mojmir.states) {
                if (!finalStates.get(fs)) {
                    for (Map.Entry<ValuationSet, FormulaAutomatonState> vsfs : slave.mojmir.transitions.row(fs)
                            .entrySet()) {
                        if (finalStates.get(vsfs.getValue())) {
                            succeedM.add(fs, vsfs.getKey());
                        }
                    }
                }
            }
        }
        // Product
        TranSet<ProductState> succeedP = new TranSet<>(valuationSetFactory);
        for (ProductState ps : product.states) {
            RankingState rs = ps.getSecondaryState(slave.mojmir.getFormula());
            if (rs != null) { // relevant slave
                for (FormulaAutomatonState fs : rs.keySet()) {
                    if (succeedM.containsKey(fs) && (rs.get(fs) == rank)) {
                        succeedP.add(ps, succeedM.get(fs));
                    }
                }
            }
        }
        // Set buy(pi)
        // Rabin
        TranSet<RankingState> buyR = new TranSet<>(valuationSetFactory);
        for (RankingState rs : slave.states) {
            for (FormulaAutomatonState fs : rs.keySet()) {
                if (rs.get(fs) < rank) {
                    for (FormulaAutomatonState fs2 : rs.keySet()) {
                        for (FormulaAutomatonState succ : slave.mojmir.states) {
                            ValuationSet vs1, vs2;
                            if (!finalStates.get(succ) && ((vs1 = slave.mojmir.edgeBetween.get(fs, succ)) != null)
                                    && ((vs2 = slave.mojmir.edgeBetween.get(fs2, succ)) != null)) {
                                if (!fs.equals(fs2)) {
                                    ValuationSet vs1copy = valuationSetFactory.createValuationSet(vs1);
                                    vs1copy.retainAll(vs2);
                                    buyR.add(rs, vs1copy);
                                } else if (succ.equals(slave.mojmir.initialState)) {
                                    buyR.add(rs, vs1);
                                }

                            }
                        }
                    }
                }
            }
        }
        // Product
        TranSet<ProductState> buyP = new TranSet<>(valuationSetFactory);
        for (ProductState ps : product.states) {
            RankingState rs = ps.getSecondaryState(slave.mojmir.getFormula());
            if (rs != null) { // relevant slave
                if (buyR.containsKey(rs)) {
                    buyP.add(ps, buyR.get(rs));
                }
            }
        }

        Main.verboseln("\tAn acceptance pair for slave " + slave.mojmir.getFormula() + ":\n" + failP + buyP + succeedP);
        return new RabinPair<>(failP.addAll(buyP), succeedP);
    }

    @Override
    public String toString() {
        return "Fin:\n" + (getLeft() == null ? "trivial" : getLeft()) + "\nInf:\n"
                + (getRight() == null ? "trivial" : getRight());
    }

}