package rabinizer.z3;

import com.microsoft.z3.Context;

import rabinizer.z3.LTLExpr;

public abstract class LTLExprBinary extends LTLExpr {
	
	public LTLExpr left;
	public LTLExpr right;
	
	
	protected LTLExprBinary(Context ctx)
    {
        super(ctx);
    }
	
	public LTLExprBinary(Context ctx,LTLExpr l,LTLExpr r)
    {
        super(ctx);
        left=l;
        right=r;
        
    }
}
