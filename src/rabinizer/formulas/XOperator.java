package rabinizer.formulas;

public class XOperator extends FormulaUnary {

    @Override
    public String operator() {
        return "X";
    }

    public XOperator(Formula f) {
        super(f);
    }

    @Override
    public XOperator ThisTypeUnary(Formula operand) {
        return new XOperator(operand);
    }

    @Override
    public Formula unfold() {
        return this;
    }

    @Override
    public Formula unfoldNoG() {
        return this;
    }

    @Override
    public Formula toNNF() {
        return new XOperator(operand.toNNF());
    }

    @Override
    public Formula negationToNNF() {
        return new XOperator(operand.negationToNNF());
    }

    //============== OVERRIDE ====================
    @Override
    public Formula removeX() {
        return operand;
    }

}
