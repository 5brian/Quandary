package ast;

import java.util.List;

public class Program extends ASTNode {
    private final String mainName;
    private final String paramName;
    private final List<Stmt> statements;

    public Program(String mainName, String paramName, List<Stmt> statements, Location loc) {
        super(loc);
        this.mainName = mainName;
        this.paramName = paramName;
        this.statements = statements;
    }

    public String getMainName() {
        return mainName;
    }

    public String getParamName() {
        return paramName;
    }

    public List<Stmt> getStatements() {
        return statements;
    }
}