package rabinizer.z3;

import com.microsoft.z3.*;

public class LTLExprTrue extends LTLExprNullary {
	
	
	protected LTLExprTrue(Context ctx)
    {
        super(ctx);
    }
	
	public LTLExprTrue(Context ctx,LTLExpr c)
    {
        super(ctx);
        
    }
}
