package rabinizer;

import rabinizer.ltl.Formula;
import rabinizer.ltl.ValuationSet;
import rabinizer.parser.LTLParser;
import rabinizer.parser.ParseException;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.fail;

public final class Util {
    public static Formula createFormula(String s) {
        LTLParser parser = new LTLParser(new StringReader(s));

        try {
            return parser.parse();
        } catch (ParseException e) {
            fail("Failed to construct formula from string");
            return null;
        }
    }

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
            list.forEach(vs::addAll);
            if (!vs.isUniverse()) {
                throw new IllegalArgumentException(vs.toString());
            }
        }
    }
}
