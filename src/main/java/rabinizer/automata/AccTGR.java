package rabinizer.automata;

import java.util.ArrayList;

public class AccTGR extends ArrayList<GRabinPair<TranSet<Product.ProductState>>> {

    private static final long serialVersionUID = 2472964991141498843L;

    public AccTGR(DTGRARaw.AccTGRRaw<Product.ProductState> accTGR) {
        for (GRabinPairRaw<Product.ProductState> grp : accTGR) {
            add(grp.order());
        }
    }

    @Override
    public String toString() {
        String result = "Gen. Rabin acceptance condition";
        int i = 1;
        for (GRabinPair<TranSet<Product.ProductState>> pair : this) {
            result += "\nPair " + i + "\n" + pair;
            i++;
        }
        return result;
    }
}
