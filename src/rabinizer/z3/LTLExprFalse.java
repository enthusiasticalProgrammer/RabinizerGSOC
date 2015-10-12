package rabinizer.z3;

import com.microsoft.z3.*;

public class LTLExprFalse extends LTLExprNullary {
	
	
	protected LTLExprFalse(Context ctx)
    {
        super(ctx);
    }
	
	public LTLExprFalse(Context ctx,LTLExpr c)
    {
        super(ctx);
        
    }
}