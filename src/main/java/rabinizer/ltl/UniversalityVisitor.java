package rabinizer.ltl;

//returns true if the Formula is pure universal
public class UniversalityVisitor implements AttributeVisitor {
    private static UniversalityVisitor instance = new UniversalityVisitor();

    // to overwrite the public default constructor-->other classes have to call
    // getVisitor()
    private UniversalityVisitor() {
        super();
    }

    public static UniversalityVisitor getVisitor() {
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
        boolean evtl = true;
        for (Formula child : d.children) {
            evtl = evtl && child.acceptBool(this);
        }
        return evtl;
    }

    public boolean visitF(FOperator f) {
        return f.operand.acceptBool(this);
    }

    public boolean visitG(GOperator g) {
        return true;
    }

    public boolean visitL(Literal l) {
        return false;
    }

    public boolean visitN(Negation n) {
        return n.operand.acceptBool(EventualVisitor.getVisitor());
    }

    public boolean visitU(UOperator u) {
        return u.left.acceptBool(this) && u.right.acceptBool(this);
    }

    public boolean visitX(XOperator x) {
        return x.operand.acceptBool(this);
    }
}
