package ast;

public class TypeCastExpr extends Expr {
    private final Type type;
    private final Expr expr;

    public TypeCastExpr(Type type, Expr expr, Location loc) {
        super(loc);
        this.type = type;
        this.expr = expr;
    }

    public Type getType() {
        return type;
    }

    public Expr getExpr() {
        return expr;
    }
}
