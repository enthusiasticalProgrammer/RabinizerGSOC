package rabinizer.z3;

import com.microsoft.z3.*;

public class LTLExprSym extends LTLExprNullary {
	Symbol sym;
	
	protected LTLExprSym(Context ctx)
    {
        super(ctx);
    }
	
	public LTLExprSym(Context ctx,LTLExpr c,String name)
    {
        super(ctx);
        sym=ctx.mkSymbol(name);
    }
}