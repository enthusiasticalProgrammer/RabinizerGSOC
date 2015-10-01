package rabinizer.bdd;

import java.util.*;
import net.sf.javabdd.*;
import rabinizer.formulas.*;

/**
 * Global state & pervasive methods.
 *
 * @author Ruslan Ledesma-Garza
 *
 */
public class BDDForFormulae {

    /**
     * The BDD factory for the purpose of constructing canonical representations
     * of formulas.
     */
    public static BDDFactory bddFactory;

    /**
     * The map from boolean atoms to BDD variables for the purpose of
     * constructing BDDs over boolean atoms.
     *
     * Populated by Formula.bdd().
     */
    public static BijectionBooleanAtomBddVar bijectionBooleanAtomBddVar;

    public static void init() {
        bijectionBooleanAtomBddVar = new BijectionBooleanAtomBddVar();
        bddFactory = BDDFactory.init("java", 100, 100);
        bddToRepresentative = new HashMap();
    }

    /**
     * Cache for the representative of a given bdd
     */
    private static Map<BDD, Formula> bddToRepresentative;

    /**
     * Return the cached representative of a BDD.
     */
    public static Formula representativeOfBdd(BDD bdd, Formula representativeCandidate) {
        if (!bddToRepresentative.containsKey(bdd)) {
            bddToRepresentative.put(bdd, representativeCandidate);
        }
        return bddToRepresentative.get(bdd);
    }

    public static BDD trueFormulaBDD() {
        return bddFactory.one();
    }

}
