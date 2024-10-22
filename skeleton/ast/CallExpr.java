package ast;

import java.util.List;

public class CallExpr extends Expr {
    private final String funcName;
    private final List<Expr> arguments;

    public CallExpr(String funcName, List<Expr> arguments, Location loc) {
        super(loc);
        this.funcName = funcName;
        this.arguments = arguments;
    }

    public String getFuncName() {
        return funcName;
    }

    public List<Expr> getArguments() {
        return arguments;
    }
}
