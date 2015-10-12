package rabinizer.z3;

import com.microsoft.z3.*;



public class LTLExprNot extends LTLExprUnary {
	
	
	
	protected LTLExprNot(Context ctx)
    {
        super(ctx);
    }
	
	public LTLExprNot(Context ctx,LTLExpr c)
    {
        super(ctx);
        child=c;
        
    }
}