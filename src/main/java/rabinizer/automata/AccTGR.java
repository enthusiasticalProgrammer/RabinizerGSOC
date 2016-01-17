package rabinizer.automata;

import java.util.ArrayList;
import java.util.Set;

public class AccTGR<S extends IState<S>> extends ArrayList<GRabinPairT<S>> {

    private static final long serialVersionUID = 2472964991141498843L;

    public AccTGR(AccTGRRaw<Product.ProductState> accTGR) {
        for (GRabinPairRaw<Product.ProductState> grp : accTGR) {
            add(grp.order());
        }
    }

    @Override
    public String toString() {
        String result = "Gen. Rabin acceptance condition";
        int i = 1;
        for (GRabinPairT pair : this) {
            result += "\nPair " + i + "\n" + pair;
            i++;
        }
        return result;
    }

    String accSets(AbstractProductState s, Set<String> v) {
        String result = "";
        int sum = 0;
        for (GRabinPairT<S> gRabinPairT : this) {
            if (gRabinPairT.left.containsKey(s) && gRabinPairT.left.get(s).contains(v)) {
                result += sum + " ";
            }
            sum++;
            for (TranSet<S> ts : gRabinPairT.right) {
                if (ts.containsKey(s) && ts.get(s).contains(v)) {
                    result += sum + " ";
                }
                sum++;
            }
        }
        return result;
    }

}
