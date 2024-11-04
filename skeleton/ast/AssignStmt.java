package ast;

public class AssignStmt extends Stmt {
    private final String name;
    private final Expr expr;

    public AssignStmt(String name, Expr expr, Location loc) {
        super(loc);
        this.name = name;
        this.expr = expr;
    }

    public String getName() {
        return name;
    }

    public Expr getExpr() {
        return expr;
    }
}