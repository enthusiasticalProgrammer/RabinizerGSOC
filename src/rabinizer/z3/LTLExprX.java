package rabinizer.z3;

import com.microsoft.z3.*;



public class LTLExprX extends LTLExprUnary {
	
	
	
	protected LTLExprX(Context ctx)
    {
        super(ctx);
    }
	
	public LTLExprX(Context ctx,LTLExpr c)
    {
        super(ctx);
        child=c;
        
    }
}