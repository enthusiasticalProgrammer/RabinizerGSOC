package rabinizer.automata.nxt;

import rabinizer.ltl.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class Util {

    public static void checkPartition(Collection<ValuationSet> set, boolean checkUniverse) {
        List<ValuationSet> list = new ArrayList<>(set);

        for (int i = 0; i < list.size(); i++) {
            for (int j = 0; j < i; j++) {
                ValuationSet s = list.get(i).clone();
                s.retainAll(list.get(j));

                if (!s.isEmpty()) {
                    throw new IllegalArgumentException(list.get(i) + " " + list.get(j));
                }
            }
        }

        if (checkUniverse) {
            ValuationSet vs = list.get(0).clone();
            list.forEach(e -> vs.addAll(e));
            if (!vs.isUniverse()) {
                throw new IllegalArgumentException(vs.toString());
            }
        }
    }
}
