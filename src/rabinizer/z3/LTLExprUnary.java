package rabinizer.z3;

import com.microsoft.z3.*;

public abstract class LTLExprUnary extends LTLExpr {
	
	public LTLExpr child;
	
	
	protected LTLExprUnary(Context ctx)
    {
        super(ctx);
    }
	
	public LTLExprUnary(Context ctx,LTLExpr c)
    {
        super(ctx);
        child=c;
        
    }
}