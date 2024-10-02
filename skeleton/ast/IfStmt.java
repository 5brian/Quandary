package ast;

public class IfStmt extends Stmt {
    private final Expr condition;
    private final Stmt thenStmt;
    private final Stmt elseStmt;

    public IfStmt(Expr condition, Stmt thenStmt, Stmt elseStmt, Location loc) {
        super(loc);
        this.condition = condition;
        this.thenStmt = thenStmt;
        this.elseStmt = elseStmt;
    }

    public Expr getCondition() {
        return condition;
    }

    public Stmt getThenStmt() {
        return thenStmt;
    }

    public Stmt getElseStmt() {
        return elseStmt;
    }
}
