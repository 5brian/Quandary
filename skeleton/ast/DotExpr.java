package ast;

public class DotExpr extends Expr {
    private final Expr left;
    private final Expr right;

    public DotExpr(Expr left, Expr right, Location loc) {
        super(loc);
        this.left = left;
        this.right = right;
    }

    public Expr getLeft() {
        return left;
    }

    public Expr getRight() {
        return right;
    }
}
