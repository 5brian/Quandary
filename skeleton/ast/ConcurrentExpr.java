package ast;

public class ConcurrentExpr extends Expr {
    private final Expr expr1;
    private final int operator;
    private final Expr expr2;

    public ConcurrentExpr(Expr expr1, int operator, Expr expr2, Location loc) {
        super(loc);
        this.expr1 = expr1;
        this.operator = operator;
        this.expr2 = expr2;
    }

    public Expr getLeft() {
        return expr1;
    }

    public int getOperator() {
        return operator;
    }

    public Expr getRight() {
        return expr2;
    }
}