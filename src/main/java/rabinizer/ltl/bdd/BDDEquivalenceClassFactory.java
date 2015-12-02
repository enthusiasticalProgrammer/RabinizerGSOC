package rabinizer.ltl.bdd;

import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import rabinizer.ltl.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BDDEquivalenceClassFactory implements EquivalenceClassFactory {

    private final BDDFactory factory;
    private final BDDVisitor visitor;
    private final Map<Formula, BDD> mapping;

    public BDDEquivalenceClassFactory(Set<Formula> domain) {
        factory = BDDFactory.init("java", 16 * domain.size(), 1000);
        factory.setVarNum(domain.size());
        mapping = new HashMap<>();

        int var = 0;

        for (Formula proposition : domain) {
            BDD pos = factory.ithVar(var);
            BDD neg = factory.nithVar(var);

            mapping.put(proposition, pos);
            mapping.put(proposition.not(), neg);

            var++;
        }

        visitor = new BDDVisitor();
    }

    public EquivalenceClass createEquivalenceClass(Formula formula) {
        return new BDDEquivalenceClass(formula, formula.accept(visitor));
    }

    private class BDDVisitor implements Visitor<BDD> {
        @Override
        public BDD visit(BooleanConstant b) {
            return b.value ? factory.one() : factory.zero();
        }

        @Override
        public BDD visit(Conjunction c) {
            return c.getChildren().stream().map(x -> x.accept(this)).reduce(factory.one(), BDD::and);
        }

        @Override
        public BDD visit(Disjunction d) {
            return d.getChildren().stream().map(x -> x.accept(this)).reduce(factory.zero(), BDD::or);
        }

        @Override
        public BDD visit(FOperator f) {
            return mapping.get(f);
        }

        @Override
        public BDD visit(GOperator g) {
            return mapping.get(g);
        }

        @Override
        public BDD visit(Literal l) {
            return mapping.get(l);
        }

        @Override
        public BDD visit(UOperator u) {
            return mapping.get(u);
        }

        @Override
        public BDD visit(XOperator x) {
            return mapping.get(x);
        }
    }
}
