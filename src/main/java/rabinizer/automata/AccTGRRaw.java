package rabinizer.automata;

import rabinizer.exec.Main;
import rabinizer.ltl.EquivalenceClassFactory;
import rabinizer.ltl.Formula;
import rabinizer.ltl.GOperator;
import rabinizer.ltl.ValuationSetFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author jkretinsky
 */
public class AccTGRRaw extends HashSet<GRabinPairRaw> {

    private static final long serialVersionUID = 245172601429256815L;
    private final TranSet<ProductState> allTrans;

    protected final ValuationSetFactory<String> valuationSetFactory;
    protected final EquivalenceClassFactory equivalenceClassFactory;

    public AccTGRRaw(ValuationSetFactory<String> factory, EquivalenceClassFactory factory2) {
        allTrans = null;
        this.valuationSetFactory = factory;
        this.equivalenceClassFactory = factory2;
    }

    public AccTGRRaw(TranSet<ProductState> allTrans, ValuationSetFactory<String> factory,
            EquivalenceClassFactory factory2) {
        this.allTrans = allTrans;
        this.valuationSetFactory = factory;
        this.equivalenceClassFactory = factory2;
    }

    public AccTGRRaw(AccTGRRaw accTGR, ValuationSetFactory<String> factory, EquivalenceClassFactory factory2) {
        super(accTGR);
        allTrans = accTGR.allTrans;
        this.valuationSetFactory = factory;
        this.equivalenceClassFactory = factory2;
    }

    public AccTGRRaw(AccLocal accLocal, ValuationSetFactory<String> factory, EquivalenceClassFactory factory2) {
        super();
        allTrans = accLocal.allTrans;
        this.valuationSetFactory = factory;
        this.equivalenceClassFactory = factory2;
        for (Set<GOperator> gSet : accLocal.accMasterOptions.keySet()) {
            Main.verboseln("\tGSet " + gSet);
            for (Map<Formula, Integer> ranking : accLocal.accMasterOptions.get(gSet).keySet()) {
                Main.verboseln("\t  Ranking " + ranking);
                TranSet<ProductState> Fin = new TranSet<>(valuationSetFactory);
                Set<TranSet<ProductState>> Infs = new HashSet<>();
                Fin.addAll((TranSet<ProductState>) accLocal.accMasterOptions.get(gSet).get(ranking).getLeft());
                for (Formula f : gSet) {
                    Set<GOperator> localGSet = new HashSet<>(gSet);
                    localGSet.retainAll(accLocal.topmostGs.get(f));
                    RabinPair fPair = accLocal.accSlavesOptions.get(f).get(localGSet).get(ranking.get(f));
                    Fin.addAll((TranSet<ProductState>) fPair.getLeft());
                    Infs.add((TranSet<ProductState>) fPair.getRight());
                }
                GRabinPairRaw pair = new GRabinPairRaw(Fin, Infs);
                Main.verboseln(pair.toString());
                this.add(pair);
            }
        }
    }

    public AccTGRRaw removeRedundancy() { // (TranSet<ProductState> allTrans) {
        AccTGRRaw removalPairs;
        AccTGRRaw temp;
        Set<TranSet<ProductState>> copy;
        int phase = 0;
        Main.stopwatchLocal();

        Main.verboseln(phase + ". Raw Generalized Rabin Acceptance Condition\n");
        // Main.verboseln(this.toString());
        printProgress(phase++);

        /*
         * is this duplicate here more efficient? not really
         * Main.verboseln(phase +
         * ". Removing (F, I) for which there is a less restrictive (G, J) \n");
         * removalPairs = new AccTGRRaw(); for (GRabinPairRaw pair1 : this) {
         * for (GRabinPairRaw pair2 : this) { if (!pair1.equals(pair2) &&
         * pairSubsumed(pair1, pair2)) { removalPairs.add(pair1); break; } } }
         * this.removeAll(removalPairs); //Main.verboseln(this.toString());
         * printProgress(phase++);
         */

        Main.verboseln(phase + ". Removing (F, {I1,...,In}) with complete F\n");
        removalPairs = new AccTGRRaw(valuationSetFactory, equivalenceClassFactory);
        for (GRabinPairRaw pair : this) {
            if (pair.getLeft().equals(allTrans)) {
                removalPairs.add(pair);
            }
        }
        this.removeAll(removalPairs);
        // Main.verboseln(this.toString());
        printProgress(phase++);

        Main.verboseln(phase + ". Removing complete Ii in (F, {I1,...,In}), i.e. Ii U F = Q \n");
        temp = new AccTGRRaw(valuationSetFactory, equivalenceClassFactory);
        for (GRabinPairRaw pair : this) {
            copy = new HashSet<>(pair.getRight());
            for (TranSet<ProductState> i : pair.getRight()) {
                TranSet<ProductState> iUf = new TranSet<>(valuationSetFactory);
                if (iUf.addAll(i).addAll(pair.getLeft()).equals(allTrans)) {
                    copy.remove(i);
                    break;
                }
            }
            temp.add(new GRabinPairRaw(pair.getLeft(), copy));
        }
        this.clear();
        this.addAll(temp);
        // Main.verboseln(this.toString());
        printProgress(phase++);

        Main.verboseln(phase + ". Removing F from each Ii: (F, {I1,...,In}) |-> (F, {I1\\F,...,In\\F})\n");
        temp = new AccTGRRaw(valuationSetFactory, equivalenceClassFactory);
        for (GRabinPairRaw pair : this) {
            copy = new HashSet<>(pair.getRight());
            for (TranSet<ProductState> i : pair.getRight()) {
                copy.remove(i); // System.out.println("101:::::::"+i);
                TranSet<ProductState> inew = new TranSet<>(valuationSetFactory);
                inew.addAll(i); // System.out.println("105TEMP-BEFORE"+temp+"\n=====");
                inew.removeAll(pair.getLeft()); // System.out.println("105TEMP-BETWEEN"+temp+"\n=====");
                copy.add(inew); // System.out.println("103TEMP-AFTER"+temp);
            }
            temp.add(new GRabinPairRaw(pair.getLeft(), copy));// System.out.println("105TEMP-AFTER"+temp+"\n=====");
        }
        this.clear();
        this.addAll(temp);
        // Main.verboseln(this.toString());
        printProgress(phase++);

        Main.verboseln(phase + ". Removing (F, {..., \\emptyset, ...} )\n");
        removalPairs = new AccTGRRaw(valuationSetFactory, equivalenceClassFactory);
        for (GRabinPairRaw pair : this) {
            for (TranSet<ProductState> i : pair.getRight()) {
                if (i.isEmpty()) {
                    removalPairs.add(pair);
                    break;
                }
            }
        }
        this.removeAll(removalPairs);
        // Main.verboseln(this.toString());
        printProgress(phase++);

        Main.verboseln(
                phase + ". Removing redundant Ii: (F, I) |-> (F, { i | i in I and !\\exists j in I : Ij <= Ii })\n");
        for (GRabinPairRaw pair : this) {
            copy = new HashSet<>(pair.getRight());
            for (TranSet<ProductState> i : pair.getRight()) {
                for (TranSet<ProductState> j : pair.getRight()) {
                    if (!j.equals(i) && j.subsetOf(i)) {
                        copy.remove(i);
                        break;
                    }
                }
            }
            temp.add(new GRabinPairRaw(pair.getLeft(), copy));
        }
        this.clear();
        this.addAll(temp);
        // Main.verboseln(this.toString());
        printProgress(phase++);

        Main.verboseln(phase + ". Removing (F, I) for which there is a less restrictive (G, J) \n");
        removalPairs = new AccTGRRaw(valuationSetFactory, equivalenceClassFactory);
        for (GRabinPairRaw pair1 : this) {
            for (GRabinPairRaw pair2 : this) {
                if (!pair1.equals(pair2) && pairSubsumed(pair1, pair2)) {
                    removalPairs.add(pair1);
                    break;
                }
            }
        }
        this.removeAll(removalPairs);
        // Main.verboseln(this.toString());
        printProgress(phase++);
        return this;
    }

    /**
     * True if pair1 is more restrictive than pair2
     */
    private boolean pairSubsumed(GRabinPairRaw pair1, GRabinPairRaw pair2) {
        if (!pair2.getLeft().subsetOf(pair1.getLeft())) {
            return false;
        }
        for (TranSet<ProductState> i2 : pair2.getRight()) {
            boolean i2CanBeMatched = false;
            for (TranSet<ProductState> i1 : pair1.getRight()) {
                if (i1.subsetOf(i2)) {
                    i2CanBeMatched = true;
                    break;
                }
            }
            if (!i2CanBeMatched) {
                return false;
            }
        }
        return true;
    }

    public void printProgress(int phase) {
        Main.nonsilent("Phase " + phase + ": " + Main.stopwatchLocal() + " s " + this.size() + " pairs");
    }

    @Override
    public String toString() {
        String result = "Gen. Rabin acceptance condition";
        int i = 1;
        for (GRabinPairRaw pair : this) {
            result += "\nPair " + i + "\n" + pair;
            i++;
        }
        return result;
    }

}
