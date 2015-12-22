package rabinizer.ltl.z3;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Model;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;

import rabinizer.ltl.BooleanConstant;
import rabinizer.ltl.Conjunction;
import rabinizer.ltl.Disjunction;
import rabinizer.ltl.Formula;
import rabinizer.ltl.Visitor;

public class Z3LibraryWrapper<F extends Formula> {
    private final Z3Visitor visitor;
    protected final BiMap<F, BoolExpr> mapping;
    private final Context ctx;
    public final BoolExpr TRUE;
    public final BoolExpr FALSE;

    public Z3LibraryWrapper(Set<F> domain) {
        visitor = new Z3Visitor();
        this.ctx = new Context();
        TRUE = ctx.mkTrue();
        FALSE = ctx.mkFalse();

        ImmutableBiMap.Builder<F, BoolExpr> builder = new ImmutableBiMap.Builder<>();
        List<BoolExpr> vars;
        vars = new ArrayList<>();

        int var = 0;
        for (F proposition : domain) {

            vars.add(mkConst(proposition.toString()));

            BoolExpr pos = vars.get(var);
            BoolExpr neg = negate(vars.get(var));

            builder.put(proposition, pos);
            builder.put((F) proposition.not(), neg);
            var++;
        }
        mapping = builder.build();
    }

    protected Formula createRepresentative(BoolExpr expression) {
        if (expression.isTrue()) {
            return BooleanConstant.TRUE;
        } else if (expression.isFalse()) {
            return BooleanConstant.FALSE;
        } else if (expression.isAnd()) {
            Expr[] exp = expression.getArgs();
            return new Disjunction(Arrays.asList(exp).stream().map(s -> createRepresentative((BoolExpr) s)));
        } else if (expression.isOr()) {
            Expr[] exp = expression.getArgs();
            return new Disjunction(Arrays.asList(exp).stream().map(s -> createRepresentative((BoolExpr) s)));
        } else if (expression.isNot()) {
            return createRepresentative((BoolExpr) expression.getArgs()[0]).not();
        } else if (expression.isConst()) {
            return mapping.inverse().get(expression);
        } else {
            throw new IllegalArgumentException("The expression does not correspond to a Formula");
        }
    }

    protected BoolExpr createZ3(Formula f) {
        return f.accept(visitor);
    }

    protected Set<F> getSatAssignment(BoolExpr expression) {
        Set<F> result = new HashSet<>();
        Solver s = ctx.mkSolver();
        s.add(expression);
        if (s.check() != Status.SATISFIABLE) {
            throw new NoSuchElementException();
        }
        Model m = s.getModel();
        m.getConstDecls();
        for (BoolExpr e : getPropositionsOutOfBoolExpr(expression)) {
            if (m.getConstInterp(e).isTrue()) {
                result.add(mapping.inverse().get(e));
            } else if (m.getConstInterp(e).isFalse()) {
                result.add(mapping.inverse().get(negate(e)));
            }
        }
        return result;
    }

    protected Set<BoolExpr> getPropositionsOutOfBoolExpr(BoolExpr bool) {

        Set<BoolExpr> result = new HashSet<>();

        if (bool.isConst()) {
            if (bool != FALSE && bool != TRUE) {
                result.add(bool);
            }
        } else {
            for (Expr exp : bool.getArgs()) {
                BoolExpr b = (BoolExpr) exp;
                result.addAll(getPropositionsOutOfBoolExpr(b));
            }
        }
        return result;
    }

    protected boolean checkImplies(BoolExpr condition, BoolExpr conclusion) {
        return testUnsatisfiability(negate(mkImp(condition, conclusion)));
    }

    protected boolean isTautology(BoolExpr taut) {
        return testUnsatisfiability(negate(taut));
    }

    protected boolean isContradiction(BoolExpr contradiction) {
        return testUnsatisfiability(contradiction);
    }

    protected boolean checkEquality(BoolExpr b1, BoolExpr b2) {
        return testUnsatisfiability(negate(ctx.mkEq(b1, b2)));
    }

    protected BoolExpr and(BoolExpr... children) {
        return (BoolExpr) ctx.mkAnd(children).simplify();
    }

    protected BoolExpr or(BoolExpr... children) {
        return (BoolExpr) ctx.mkOr(children).simplify();
    }

    protected BoolExpr negate(BoolExpr child) {
        return (BoolExpr) ctx.mkNot(child).simplify();
    }

    protected BoolExpr mkImp(BoolExpr condition, BoolExpr consequence) {
        return ctx.mkImplies(condition, consequence);
    }

    protected BoolExpr mkConst(String s) {
        return ctx.mkBoolConst(s);
    }

    private boolean testUnsatisfiability(BoolExpr b) {
        Solver s = ctx.mkSolver();
        s.add(b);
        return s.check() == Status.UNSATISFIABLE;
    }

    private class Z3Visitor implements Visitor<BoolExpr> {

        @Override
        public BoolExpr visit(BooleanConstant b) {
            return b.value ? TRUE : FALSE;
        }

        @Override
        public BoolExpr visit(Conjunction c) {
            if (c.getChildren().contains(BooleanConstant.FALSE)) {
                return FALSE;
            }

            BoolExpr[] b = new BoolExpr[c.getChildren().size()];
            int index = 0;
            for (Formula elem : c.getChildren()) {
                b[index++] = elem.accept(this);
            }
            return ctx.mkAnd(b);
        }

        @Override
        public BoolExpr visit(Disjunction d) {
            if (d.getChildren().contains(BooleanConstant.TRUE)) {
                return TRUE;
            }

            BoolExpr[] b = new BoolExpr[d.getChildren().size()];

            int index = 0;
            for (Formula elem : d.getChildren()) {
                b[index++] = elem.accept(this);
            }
            return ctx.mkOr(b);
        }

        @Override
        public BoolExpr defaultAction(Formula f) {
            return mapping.get(f);
        }
    }

}
