package ast;

import java.io.PrintStream;

public class Program extends ASTNode {

    final ReturnStmt returnStmt;

    public Program(ReturnStmt returnStmt, Location loc) {
        super(loc);
        this.returnStmt = returnStmt;
    }

    public ReturnStmt getReturnStmt() {
        return returnStmt;
    }

    public void println(PrintStream ps) {
        ps.println(returnStmt.getExpr());
    }
}