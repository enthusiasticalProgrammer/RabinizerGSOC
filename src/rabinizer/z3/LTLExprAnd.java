package rabinizer.z3;

import com.microsoft.z3.*;

public class LTLExprAnd extends LTLExprBinary {
	
	
	
	protected LTLExprAnd(Context ctx)
    {
        super(ctx);
    }
	
	public LTLExprAnd(Context ctx,LTLExpr l,LTLExpr r)
    {
        super(ctx);
        left=l;
        right=r;
        
    }

	
	
}
