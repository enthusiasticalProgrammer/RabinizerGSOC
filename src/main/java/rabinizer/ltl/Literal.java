package rabinizer.ltl;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import net.sf.javabdd.BDD;
import rabinizer.ltl.bdd.BDDForFormulae;
import rabinizer.ltl.bdd.Valuation;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class Literal extends FormulaNullary {

    final String atom;
    final int atomId;
    final boolean negated;

    public Literal(String atom, int atomId, boolean negated) {
        this.atom = atom;
        this.atomId = atomId;
        this.negated = negated;
    }

    public boolean getNegated() {
        return negated;
    }

    public Literal positiveLiteral() {
        return (Literal) FormulaFactory.mkLit(this.atom, this.atomId, false);
    }

    @Override
    public BDD bdd() {
        if (cachedBdd == null) {
            int bddVar = BDDForFormulae.bijectionBooleanAtomBddVar.id(this.positiveLiteral()); // R3:
                                                                                               // just
                                                                                               // "this"
            if (BDDForFormulae.bddFactory.varNum() <= bddVar) {
                BDDForFormulae.bddFactory.extVarNum(1);
            }
            cachedBdd = (negated ? BDDForFormulae.bddFactory.nithVar(bddVar)
                    : BDDForFormulae.bddFactory.ithVar(bddVar));
            BDDForFormulae.representativeOfBdd(cachedBdd, this);
        }
        return cachedBdd;

    }

    @Override
    public String toString() {
        if (cachedString == null) {
            cachedString = (negated ? "!" : "") + atom;
        }
        return cachedString;
    }

    @Override
    public Formula evaluateValuation(Valuation valuation) {
        return FormulaFactory.mkConst(valuation.get(atomId) ^ negated);
    }

    @Override
    public Formula evaluateLiteral(Literal literal) {
        if (literal.atomId != this.atomId) {
            return this;
        } else {
            return FormulaFactory.mkConst(literal.negated == this.negated);
        }
    }

    @Override
    public Literal not() {
        return new Literal(atom, atomId, !negated);
    }

    @Override
    public Literal getAnUnguardedLiteral() {
        return this;
    }

    @Override
    public BoolExpr toExpr(Context ctx) {

        if (cachedLTL == null) {
            cachedLTL = ctx.mkBoolConst(atom);
            if (negated) {
                cachedLTL = ctx.mkNot(cachedLTL);
            }
        }
        return cachedLTL;
    }

    @Override
    public Set<Formula> getPropositions() {
        Set<Formula> propositions = new HashSet<>();
        propositions.add(this);
        return propositions;
    }

    @Override
    public Formula rmAllConstants() {
        return FormulaFactory.mkLit(atom, atomId, negated);

    }

    public String getAtom() {
        return atom;
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }

    @Override
    public <A, B> A accept(BinaryVisitor<A, B> v, B f) {
        return v.visit(this, f);
    }

    @Override
    public <A, B, C> A accept(TripleVisitor<A, B, C> v, B f, C c) {
        return v.visit(this, f, c);
    }

    @Override
    public boolean isPureEventual() {
        return false;
    }

    @Override
    public boolean isPureUniversal() {
        return false;
    }

    @Override
    public boolean isSuspendable() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Literal literal = (Literal) o;
        return atomId == literal.atomId && negated == literal.negated && Objects.equals(atom, literal.atom);
    }

    @Override
    protected int hashCodeOnce() {
        return Objects.hash(atom, atomId, negated);
    }
}
