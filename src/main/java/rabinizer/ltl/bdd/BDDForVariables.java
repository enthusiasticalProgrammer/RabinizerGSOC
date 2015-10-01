package rabinizer.ltl.bdd;

import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import rabinizer.ltl.Literal;

/**
 * Service class that serves as an interface to javabdd.
 *
 * @author andreas
 */
public class BDDForVariables {

    public static int numOfVariables;
    public static String[] variables;
    /**
     * The bijection between atom identifiers and atoms for the purpose of 1.
     * Consistently creating literals across parsing of different ltl (e.g.
     * during unit testing) by mapping atoms to identifiers 2. Rendering
     * valuations by mapping identifiers to atoms
     * <p>
     * Created and populated during parsing.
     */
    public static BijectionIdAtom bijectionIdAtom = new BijectionIdAtom();
    private static BDDFactory bf;

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
        if (!l.getNegated()) {
            return bf.ithVar(bijectionIdAtom.id(l.getAtom()));
        } else {
            return bf.nithVar(bijectionIdAtom.id(l.getAtom()));
        }
    }

}
