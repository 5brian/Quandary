package ast;

public class VarDecl extends Stmt {
    private final String name;
    private final Type type;
    private final Expr initExpr;
    private final boolean isMutable;

    public VarDecl(String name, Type type, Expr initExpr, boolean isMutable, Location loc) {
        super(loc);
        this.name = name;
        this.type = type;
        this.initExpr = initExpr;
        this.isMutable = isMutable;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public Expr getInitExpr() {
        return initExpr;
    }

    public boolean isMutable() {
        return isMutable;
    }
}