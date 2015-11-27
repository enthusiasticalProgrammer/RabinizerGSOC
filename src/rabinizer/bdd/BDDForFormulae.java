package rabinizer.bdd;

import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import rabinizer.formulas.Formula;

import java.util.HashMap;
import java.util.Map;

/**
 * Global state & pervasive methods.
 *
 * @author Ruslan Ledesma-Garza
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
     * <p>
     * Populated by Formula.bdd().
     */
    public static BijectionBooleanAtomBddVar bijectionBooleanAtomBddVar;
    /**
     * Cache for the representative of a given bdd
     */
    private static Map<BDD, Formula> bddToRepresentative;

    public static void init() {
        bijectionBooleanAtomBddVar = new BijectionBooleanAtomBddVar();
        bddFactory = BDDFactory.init("java", 100, 100);
        bddToRepresentative = new HashMap<>();
    }

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
