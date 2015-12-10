package rabinizer.ltl;

import rabinizer.parser.LTLParser;
import rabinizer.parser.ParseException;

import java.io.StringReader;

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
}
