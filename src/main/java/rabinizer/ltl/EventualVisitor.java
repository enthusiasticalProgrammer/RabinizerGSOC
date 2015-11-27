package rabinizer.ltl;

//visit returns true if formula is pure_eventual
public class EventualVisitor implements AttributeVisitor {

    private static EventualVisitor instance = new EventualVisitor();

    // to overwrite the public default constructor-->other classes have to call
    // getVisitor()
    private EventualVisitor() {
        super();
    }

    public static EventualVisitor getVisitor() {
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

    public boolean visitG(GOperator g) {
        return g.operand.acceptBool(this);
    }

    public boolean visitL(Literal l) {
        return false;
    }

    public boolean visitN(Negation n) {
        return n.operand.acceptBool(UniversalityVisitor.getVisitor());
    }

    public boolean visitU(UOperator u) {
        return u.right.acceptBool(this);
    }

    public boolean visitX(XOperator x) {
        return x.operand.acceptBool(this);
    }

    public boolean visitF(FOperator f) {
        return true;
    }

}
