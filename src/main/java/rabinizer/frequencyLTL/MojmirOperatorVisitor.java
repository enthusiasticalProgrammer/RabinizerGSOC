package rabinizer.frequencyLTL;

import ltl.visitors.DefaultConverter;
import ltl.FOperator;

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
}
