package rabinizer.ltl;

public class PatientSlaveVisitor implements Visitor<Boolean> {

    @Override
    public Boolean visit(XOperator x) {
        return true;
    }

    @Override
    public Boolean visit(Conjunction p) {
        return p.children.stream().allMatch(child -> child.accept(this));
    }

    @Override
    public Boolean visit(Disjunction p) {
        return p.children.stream().allMatch(child -> child.accept(this));
    }

    @Override
    public Boolean defaultAction(Formula formula) {
        return false;
    }
}
