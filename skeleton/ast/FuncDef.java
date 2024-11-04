package ast;

import java.util.List;

public class FuncDef extends ASTNode {
    private final String name;
    private final Type returnType;
    private final boolean isMutable;
    private final List<VarDecl> params;
    private final List<Stmt> body;

    public FuncDef(String name, Type returnType, boolean isMutable, List<VarDecl> params, List<Stmt> body,
            Location loc) {
        super(loc);
        this.name = name;
        this.returnType = returnType;
        this.isMutable = isMutable;
        this.params = params;
        this.body = body;
    }

    public String getName() {
        return name;
    }

    public Type getReturnType() {
        return returnType;
    }

    public boolean isMutable() {
        return isMutable;
    }

    public List<VarDecl> getParams() {
        return params;
    }

    public List<Stmt> getBody() {
        return body;
    }
}