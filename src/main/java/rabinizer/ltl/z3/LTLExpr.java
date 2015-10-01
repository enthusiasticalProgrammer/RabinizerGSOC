package rabinizer.ltl.z3;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import rabinizer.ltl.Formula;

/**
 * @author Christopher Ziegler
 */

public class LTLExpr {

    /**
     * needed in order to communicate properly with the Z3-Solver, e.g. to store
     * the atomar propositions
     */
    private static Context ctx = null;

    /**
     * propositional formula, in order to have a representative with which you
     * can compare two formulae propositionally
     */
    private BoolExpr propForm;

    public LTLExpr(Formula f) {
        if (ctx == null) {
            ctx = new Context();
        }
        propForm = f.toExpr(ctx);
        propForm.simplify();
    }

    public static void dispose() {
        ctx.dispose();
        ctx = null;
    }

    public static Context getContext() {
        if (ctx == null) {
            ctx = new Context();
        }
        return ctx;
    }

}
