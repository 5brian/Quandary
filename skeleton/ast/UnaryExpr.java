package ast;

public class UnaryExpr extends Expr {
    public static final int MINUS = 1;
    public static final int NOT = 2;

    final int operator;
    final Expr expr;

    public UnaryExpr(int operator, Expr expr, Location loc) {
        super(loc);
        this.operator = operator;
        this.expr = expr;
    }

    public int getOperator() {
        return operator;
    }

    public Expr getExpr() {
        return expr;
    }

    // @Override
    // public String toString() {
    // String s = null;
    // switch (operator) {
    // case MINUS:
    // s = "-";
    // break;
    // }
    // return s + "(" + expr + ")";
    // }

}