package ast;

import java.util.List;

public class Program extends ASTNode {
    private final List<FuncDef> funcDefList;

    public Program(List<FuncDef> funcDefList, Location loc) {
        super(loc);
        this.funcDefList = funcDefList;
    }

    public List<FuncDef> getFuncDefList() {
        return funcDefList;
    }
}