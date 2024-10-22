package ast;

import java.util.List;

public class FuncDef extends ASTNode {
    private final String name;
    private final List<VarDecl> params;
    private final List<Stmt> body;

    public FuncDef(String name, List<VarDecl> params, List<Stmt> body, Location loc) {
        super(loc);
        this.name = name;
        this.params = params;
        this.body = body;
    }

    public String getName() {
        return name;
    }

    public List<VarDecl> getParams() {
        return params;
    }

    public List<Stmt> getBody() {
        return body;
    }
}
