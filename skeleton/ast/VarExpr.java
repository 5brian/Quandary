package ast;

public class VarExpr extends Expr {
    private final String name;

    public VarExpr(String name, Location loc) {
        super(loc);
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
