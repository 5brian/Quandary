package ast;

import java.util.List;

public class CallStmt extends Stmt {
    private final String name;
    private final List<Expr> args;

    public CallStmt(String name, List<Expr> args, Location loc) {
        super(loc);
        this.name = name;
        this.args = args;
    }

    public String getName() {
        return name;
    }

    public List<Expr> getArgs() {
        return args;
    }
}