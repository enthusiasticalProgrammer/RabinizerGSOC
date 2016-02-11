package rabinizer.ltl;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class RelevantGFormulaeWithSlaveSuspension implements Visitor<Boolean> {

    @Override
    public Boolean defaultAction(Formula formula) {
        return false;
    }

    @Override
    public Boolean visit(GOperator g) {
        return true;
    }

    @Override
    public Boolean visit(FOperator f) {
        return f.operand.accept(this);
    }

    @Override
    public Boolean visit(UOperator u) {
        return u.left.accept(this) || u.right.accept(this);
    }

    @Override
    public Boolean visit(Conjunction c) {
        Set<Formula> canBeWaited = c.children.stream().filter(child -> !child.accept(new ContainsVisitor(GOperator.class))).collect(Collectors.toSet());
        c.children.stream().filter(child -> child.accept(new PatientSlaveVisitor())).forEach(canBeWaited::add);
        Set<Formula> relevantChildren = new HashSet<>(c.children);
        relevantChildren.removeIf(child -> child.accept(new PatientSlaveVisitor()));

        if (!canBeWaited.isEmpty()) {
            relevantChildren = relevantChildren.stream().filter(child -> !child.isSuspendable())
                    .collect(Collectors.toSet());
        }

        return relevantChildren.stream().anyMatch(child -> child.accept(this));
    }

    @Override
    public Boolean visit(Disjunction d) {
        Set<Formula> patientFormulae = d.children.stream().filter(child -> child.accept(new PatientSlaveVisitor()))
                .collect(Collectors.toSet());

        Set<Formula> relevantChildren = new HashSet<>(d.children);
        relevantChildren.removeIf(child -> child.accept(new PatientSlaveVisitor()));

        if (!patientFormulae.isEmpty()) {
            relevantChildren = relevantChildren.stream().filter(child -> !child.isSuspendable())
                    .collect(Collectors.toSet());
        }

        return relevantChildren.stream().anyMatch(child -> child.accept(this));
    }
}
