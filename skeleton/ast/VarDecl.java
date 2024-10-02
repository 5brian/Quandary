package ast;

public class VarDecl extends Stmt {
    private final String name;
    private final Expr initExpr;

    public VarDecl(String name, Expr initExpr, Location loc) {
        super(loc);
        this.name = name;
        this.initExpr = initExpr;
    }

    public String getName() {
        return name;
    }

    public Expr getInitExpr() {
        return initExpr;
    }
}
