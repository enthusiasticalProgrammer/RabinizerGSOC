package rabinizer.bdd;

import net.sf.javabdd.*;
import rabinizer.formulas.Literal;

/**
 * Service class that serves as an interface to javabdd.
 *
 * @author andreas
 *
 */
public class BDDForVariables {

    private static BDDFactory bf;
    public static int numOfVariables;
    public static String[] variables;

    /**
     * The bijection between atom identifiers and atoms for the purpose of 1.
     * Consistently creating literals across parsing of different formulas (e.g.
     * during unit testing) by mapping atoms to identifiers 2. Rendering
     * valuations by mapping identifiers to atoms
     *
     * Created and populated during parsing.
     */
    public static BijectionIdAtom bijectionIdAtom = new BijectionIdAtom();

    public static void init() {
        numOfVariables = bijectionIdAtom.size();
        if (numOfVariables == 0) { // for formulae with no variables
            bijectionIdAtom.id("dummy");
            numOfVariables++;
        }
        bf = BDDFactory.init("java", numOfVariables + 1000, 1000);
        bf.setVarNum(numOfVariables);
        variables = new String[numOfVariables];
        for (int i = 0; i < numOfVariables; i++) {
            variables[i] = bijectionIdAtom.atom(i);
        }
    }

    public static BDD getTrueBDD() {
        return bf.one();
    }

    public static BDD getFalseBDD() {
        return bf.zero();
    }

    public static BDD variableToBDD(int i) {
        return bf.ithVar(i);
    }

    public static BDD variableToBDD(String s) {
        return bf.ithVar(bijectionIdAtom.id(s));
    }

    public static BDD literalToBDD(Literal l) {
        if (!l.negated) {
            return bf.ithVar(bijectionIdAtom.id(l.atom));
        } else {
            return bf.nithVar(bijectionIdAtom.id(l.atom));
        }
    }

}
