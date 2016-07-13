package rabinizer.frequencyLTL;

import ltl.visitors.DefaultConverter;
import ltl.FOperator;
import ltl.Formula;
import ltl.FrequencyG;
import ltl.GOperator;

/**
 * This Visitor visits a formula and it replaces 'ordinary' F-operators by
 * FOperatorForMojmir. The motivation is that for FOperatorForMojmir, we make a
 * Mojmir-automaton and for ordinary FOperators not.
 */
public class MojmirOperatorVisitor extends DefaultConverter {

    @Override
    public FOperatorForMojmir visit(FOperator fOperator) {
        return new FOperatorForMojmir(fOperator.operand.accept(this));
    }

    @Override
    public Formula visit(GOperator gOperator) {
        if (gOperator instanceof FrequencyG) {
            FrequencyG freq = (FrequencyG) gOperator;
            return new FrequencyG(gOperator.operand.accept(this), freq.bound, freq.cmp, freq.limes);
        }
        return super.visit(gOperator);
    }
}
