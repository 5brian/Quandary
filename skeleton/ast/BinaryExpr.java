package ast;

public class BinaryExpr extends Expr {

    public static final int PLUS = 1;
    public static final int MINUS = 2;
    public static final int TIMES = 3;
    public static final int LT = 4;
    public static final int GT = 5;
    public static final int LEQ = 6;
    public static final int GEQ = 7;
    public static final int EQEQ = 8;
    public static final int NEQ = 9;
    public static final int AND = 10;
    public static final int OR = 11;
    public static final int DOT = 12;

    final Expr expr1;
    final int operator;
    final Expr expr2;

    public BinaryExpr(Expr expr1, int operator, Expr expr2, Location loc) {
        super(loc);
        this.expr1 = expr1;
        this.operator = operator;
        this.expr2 = expr2;
    }

    public Expr getLeftExpr() {
        return expr1;
    }

    public int getOperator() {
        return operator;
    }

    public Expr getRightExpr() {
        return expr2;
    }

    // @Override
    // public String toString() {
    // String s = null;
    // switch (operator) {
    // case PLUS:
    // s = "+";
    // break;
    // case MINUS:
    // s = "-";
    // break;
    // }
    // return "(" + expr1 + " " + s + " " + expr2 + ")";
    // }
}
