package rabinizer.ltl;

public class Suspendable implements AttributeVisitor {

    private static Suspendable instance = new Suspendable();

    // to overwrite the public default constructor-->other classes have to call
    // getVisitor()
    private Suspendable() {
        super();
    }

    public static Suspendable getVisitor() {
        return instance;
    }

    public boolean visitB(BooleanConstant b) {
        return false;
    }

    public boolean visitC(Conjunction c) {
        boolean evtl = true;
        for (Formula child : c.children) {
            evtl = evtl && child.acceptBool(this);
        }
        return evtl;
    }

    public boolean visitD(Disjunction d) {
        return d.children.stream().allMatch(c -> c.acceptBool(this));
    }

    public boolean visitF(FOperator f) {
        return f.operand.acceptBool(this) || f.operand.acceptBool(UniversalityVisitor.getVisitor());
    }

    public boolean visitG(GOperator g) {
        return g.operand.acceptBool(this) || g.operand.acceptBool(EventualVisitor.getVisitor());
    }

    public boolean visitL(Literal l) {
        return false;
    }

    public boolean visitN(Negation n) {
        return n.operand.acceptBool(this);
    }

    public boolean visitU(UOperator u) {
        return u.right.acceptBool(this);
    }

    public boolean visitX(XOperator x) {
        return x.operand.acceptBool(this);
    }

}
