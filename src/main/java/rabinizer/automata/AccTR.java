package rabinizer.automata;

import rabinizer.collections.Tuple;
import rabinizer.ltl.ValuationSet;
import rabinizer.ltl.ValuationSetFactory;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/**
 * @author jkretinsky
 */
public class AccTR<T extends IState<T>> extends ArrayList<RabinPair<T>> {

    /**
     *
     */
    private static final long serialVersionUID = -5442515295977731129L;

    public AccTR(AccTGR<T> accTGR, DTRA<T> dtra, ValuationSetFactory<String> valuationSetFactory) {
        super();
        for (int i = 0; i < accTGR.size(); i++) {
            GRabinPairT<T> grp = accTGR.get(i);
            TranSet fin = new TranSet(valuationSetFactory);
            TranSet inf = new TranSet(valuationSetFactory);
            for (DTRA.ProductDegenState s : dtra.getStates()) {
                ValuationSet vsFin = grp.left.get(((Tuple<?, ?>) s).left);
                if (vsFin != null) {
                    fin.add(s, vsFin);
                }
                if (((Tuple<?, Map<Integer, Integer>>) s).right.get(i) == grp.right.size()) {
                    inf.add(s, valuationSetFactory.createUniverseValuationSet());
                }
            }
            this.add(new RabinPair(fin, inf));
        }
    }

    @Override
    public String toString() {
        String result = "Rabin transition-based acceptance condition";
        int i = 1;
        for (RabinPair pair : this) {
            result += "\nPair " + i + "\n" + pair;
            i++;
        }
        return result;
    }

    String accSets(IState s, Set<String> v) {
        String result = "";
        for (int i = 0; i < size(); i++) {
            if (get(i).left.containsKey(s) && get(i).left.get(s).contains(v)) {
                result += 2 * i + " ";
            }
            if (get(i).right.containsKey(s) && get(i).right.get(s).contains(v)) {
                result += (2 * i + 1) + " ";
            }
        }
        return result;
    }

}
