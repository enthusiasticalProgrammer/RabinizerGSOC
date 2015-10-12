package rabinizer.z3;

import com.microsoft.z3.*;

public class LTLExprF extends LTLExprUnary {
	
	
	
	protected LTLExprF(Context ctx)
    {
        super(ctx);
    }
	
	public LTLExprF(Context ctx,LTLExpr c)
    {
        super(ctx);
        child=c;
        
    }
}