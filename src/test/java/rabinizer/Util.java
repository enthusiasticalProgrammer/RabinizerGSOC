package rabinizer;

import com.google.common.collect.BiMap;
import rabinizer.ltl.Formula;
import rabinizer.collections.valuationset.ValuationSet;
import rabinizer.ltl.parser.LTLParser;
import rabinizer.ltl.parser.ParseException;

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

    public static Formula createFormula(String s, BiMap<String, Integer> mapping) {
        LTLParser parser = new LTLParser(new StringReader(s));

        if (mapping != null) {
            parser.map = mapping;
        }

        try {
            return parser.parse();
        } catch (ParseException e) {
            fail("Failed to construct formula from string: " + e.getMessage());
            return null;
        }
    }
}
