package rabinizer.z3;

import com.microsoft.z3.*;



public class LTLExprG extends LTLExprUnary {
	
	
	
	protected LTLExprG(Context ctx)
    {
        super(ctx);
    }
	
	public LTLExprG(Context ctx,LTLExpr c)
    {
        super(ctx);
        child=c;
        
    }
}