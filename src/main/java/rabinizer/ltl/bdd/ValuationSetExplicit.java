/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rabinizer.ltl.bdd;

import net.sf.javabdd.BDD;
import rabinizer.ltl.Formula;
import rabinizer.ltl.FormulaFactory;
import rabinizer.ltl.Literal;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author jkretinsky
 */
public class ValuationSetExplicit extends ValuationSet {

    private Set<Valuation> valuations;

    public ValuationSetExplicit() {
        valuations = new HashSet<>();
    }

    public ValuationSetExplicit(ValuationSet vs) {
        valuations = vs.toSet();
    }

    public ValuationSetExplicit(Set<Valuation> vs) {
        valuations = vs;
    }

    public ValuationSetExplicit(Valuation v) {
        this();
        valuations.add(v);
    }

    @Override
    public Formula toFormula() {
        Formula result = FormulaFactory.mkConst(false);
        for (Valuation v : valuations) {
            result = (FormulaFactory.mkOr(result, v.toFormula())).representative();
        }
        return result.representative();
    }

    @Override
    public BDD toBdd() {
        BDD result = BDDForVariables.getFalseBDD();
        for (Valuation v : valuations) {
            result = result.or(v.toValuationBDD());
        }
        return result;
    }

    @Override
    public Set<Valuation> toSet() {
        return valuations;
    }

    @Override
    public Valuation pickAny() {
        return valuations.iterator().next();
    }

    @Override
    public ValuationSet add(ValuationSet vs) {
        valuations.addAll(vs.toSet());
        return this;
    }


    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ValuationSet)) {
            return false;
        } else {
            return ((ValuationSet) o).toSet().equals(this.toSet());
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.valuations);
    }

    @Override
    public boolean contains(Valuation v) {
        return valuations.contains(v);
    }

    @Override
    public ValuationSet and(Literal literal) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ValuationSet and(ValuationSet vs) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ValuationSet or(ValuationSet vs) {
        Set<Valuation> union = new HashSet<>(valuations);
        union.addAll(vs.toSet());
        return new ValuationSetExplicit(union);
    }

    @Override
    public boolean isAllVals() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean contains(ValuationSet vs) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void remove(ValuationSet vs) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ValuationSet complement() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
