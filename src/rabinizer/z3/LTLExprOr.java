package rabinizer.z3;

import com.microsoft.z3.*;

public class LTLExprOr extends LTLExprBinary {
	
	
	
	protected LTLExprOr(Context ctx)
    {
        super(ctx);
    }
	
	public LTLExprOr(Context ctx,LTLExpr l,LTLExpr r)
    {
        super(ctx);
        left=l;
        right=r;
        
    }

}