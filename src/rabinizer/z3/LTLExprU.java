package rabinizer.z3;

import com.microsoft.z3.*;

public class LTLExprU extends LTLExprBinary{

	
	protected LTLExprU(Context ctx)
    {
        super(ctx);
    }
	
	public LTLExprU(Context ctx,LTLExpr l,LTLExpr r)
    {
        super(ctx);
        left=l;
        right=r;
        
    }
}
