package rabinizer.z3;

import com.microsoft.z3.*;


public abstract class LTLExprNullary extends LTLExpr {
	
	protected LTLExprNullary(Context ctx)
    {
        super(ctx);
    }
	
	public LTLExprNullary(Context ctx,LTLExpr c)
    {
        super(ctx);
        
    }
	
	
}
