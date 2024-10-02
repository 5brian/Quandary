package interpreter;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import parser.ParserWrapper;
import ast.*;

public class Interpreter {

    // Process return codes
    public static final int EXIT_SUCCESS = 0;
    public static final int EXIT_PARSING_ERROR = 1;
    public static final int EXIT_STATIC_CHECKING_ERROR = 2;
    public static final int EXIT_DYNAMIC_TYPE_ERROR = 3;
    public static final int EXIT_NIL_REF_ERROR = 4;
    public static final int EXIT_QUANDARY_HEAP_OUT_OF_MEMORY_ERROR = 5;
    public static final int EXIT_DATA_RACE_ERROR = 6;
    public static final int EXIT_NONDETERMINISM_ERROR = 7;

    static private Interpreter interpreter;

    public static Interpreter getInterpreter() {
        return interpreter;
    }

    public static void main(String[] args) {
        String gcType = "NoGC"; // default for skeleton, which only supports NoGC
        long heapBytes = 1 << 14;
        int i = 0;
        String filename;
        long quandaryArg;
        try {
            for (; i < args.length; i++) {
                String arg = args[i];
                if (arg.startsWith("-")) {
                    if (arg.equals("-gc")) {
                        gcType = args[i + 1];
                        i++;
                    } else if (arg.equals("-heapsize")) {
                        heapBytes = Long.valueOf(args[i + 1]);
                        i++;
                    } else {
                        throw new RuntimeException("Unexpected option " + arg);
                    }
                } else {
                    if (i != args.length - 2) {
                        throw new RuntimeException("Unexpected number of arguments");
                    }
                    break;
                }
            }
            filename = args[i];
            quandaryArg = Long.valueOf(args[i + 1]);
        } catch (Exception ex) {
            System.out.println("Expected format: quandary [OPTIONS] QUANDARY_PROGRAM_FILE INTEGER_ARGUMENT");
            System.out.println("Options:");
            System.out.println("  -gc (MarkSweep|Explicit|NoGC)");
            System.out.println("  -heapsize BYTES");
            System.out.println("BYTES must be a multiple of the word size (8)");
            return;
        }

        Program astRoot = null;
        Reader reader;
        try {
            reader = new BufferedReader(new FileReader(filename));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        try {
            astRoot = ParserWrapper.parse(reader);
        } catch (Exception ex) {
            ex.printStackTrace();
            Interpreter.fatalError("Uncaught parsing error: " + ex, Interpreter.EXIT_PARSING_ERROR);
        }
        // astRoot.println(System.out);
        interpreter = new Interpreter(astRoot);
        interpreter.initMemoryManager(gcType, heapBytes);
        String returnValueAsString = interpreter.executeRoot(astRoot, quandaryArg).toString();
        System.out.println("Interpreter returned " + returnValueAsString);
    }

    final Program astRoot;
    final Random random;
    private final Map<String, Object> variables = new HashMap<>();

    private Interpreter(Program astRoot) {
        this.astRoot = astRoot;
        this.random = new Random();
    }

    void initMemoryManager(String gcType, long heapBytes) {
        if (gcType.equals("Explicit")) {
            throw new RuntimeException("Explicit not implemented");
        } else if (gcType.equals("MarkSweep")) {
            throw new RuntimeException("MarkSweep not implemented");
        } else if (gcType.equals("RefCount")) {
            throw new RuntimeException("RefCount not implemented");
        } else if (gcType.equals("NoGC")) {
            // Nothing to do
        }
    }

    Object executeRoot(Program astRoot, long arg) {
        variables.put(astRoot.getParamName(), arg);

        for (Stmt stmt : astRoot.getStatements()) {
            Object result = executeStatement(stmt);
            if (result instanceof ReturnValue) {
                return ((ReturnValue) result).getValue();
            }
        }

        throw new RuntimeException("Main function must end with a return statement");
    }

    Object executeStatement(Stmt stmt) {
        if (stmt instanceof VarDecl) {
            VarDecl varDecl = (VarDecl) stmt;
            variables.put(varDecl.getName(), evaluate(varDecl.getInitExpr()));
        } else if (stmt instanceof PrintStmt) {
            PrintStmt printStmt = (PrintStmt) stmt;
            System.out.println(evaluate(printStmt.getExpr()));
        } else if (stmt instanceof IfStmt) {
            IfStmt ifStmt = (IfStmt) stmt;
            if ((Boolean) evaluate(ifStmt.getCondition())) {
                return executeStatement(ifStmt.getThenStmt());
            } else if (ifStmt.getElseStmt() != null) {
                return executeStatement(ifStmt.getElseStmt());
            }
        } else if (stmt instanceof WhileStmt) {
            WhileStmt whileStmt = (WhileStmt) stmt;
            while ((Boolean) evaluate(whileStmt.getCondition())) {
                Object result = executeStatement(whileStmt.getBody());
                if (result instanceof ReturnValue) {
                    return result;
                }
            }
        } else if (stmt instanceof ReturnStmt) {
            ReturnStmt returnStmt = (ReturnStmt) stmt;
            return new ReturnValue(evaluate(returnStmt.getExpr()));
        } else if (stmt instanceof BlockStmt) {
            BlockStmt blockStmt = (BlockStmt) stmt;
            for (Stmt s : blockStmt.getStatements()) {
                Object result = executeStatement(s);
                if (result instanceof ReturnValue) {
                    return result;
                }
            }
        }
        return null;
    }

    Object evaluate(Expr expr) {
        if (expr instanceof ConstExpr) {
            return ((ConstExpr) expr).getValue();
        } else if (expr instanceof VarExpr) {
            String name = ((VarExpr) expr).getName();
            if (!variables.containsKey(name)) {
                throw new RuntimeException("Undefined variable: " + name);
            }
            return variables.get(name);
        } else if (expr instanceof BinaryExpr) {
            BinaryExpr binaryExpr = (BinaryExpr) expr;
            Object left = evaluate(binaryExpr.getLeftExpr());
            Object right = evaluate(binaryExpr.getRightExpr());

            switch (binaryExpr.getOperator()) {
                case BinaryExpr.PLUS:
                    return (Long) left + (Long) right;
                case BinaryExpr.MINUS:
                    return (Long) left - (Long) right;
                case BinaryExpr.TIMES:
                    return (Long) left * (Long) right;
                case BinaryExpr.LT:
                    return (Long) left < (Long) right;
                case BinaryExpr.GT:
                    return (Long) left > (Long) right;
                case BinaryExpr.LEQ:
                    return (Long) left <= (Long) right;
                case BinaryExpr.GEQ:
                    return (Long) left >= (Long) right;
                case BinaryExpr.EQEQ:
                    return left.equals(right);
                case BinaryExpr.NEQ:
                    return !left.equals(right);
                case BinaryExpr.AND:
                    return (Boolean) left && (Boolean) right;
                case BinaryExpr.OR:
                    return (Boolean) left || (Boolean) right;
                default:
                    throw new RuntimeException("Unknown binary operator");
            }
        } else if (expr instanceof UnaryExpr) {
            UnaryExpr unaryExpr = (UnaryExpr) expr;
            Object operand = evaluate(unaryExpr.getExpr());
            switch (unaryExpr.getOperator()) {
                case UnaryExpr.MINUS:
                    return -(Long) operand;
                case UnaryExpr.NOT:
                    return !(Boolean) operand;
                default:
                    throw new RuntimeException("Unknown unary operator");
            }
        }
        throw new RuntimeException("Unknown expression type");
    }

    private static class ReturnValue {
        private final Object value;

        ReturnValue(Object value) {
            this.value = value;
        }

        Object getValue() {
            return value;
        }
    }

    public static void fatalError(String message, int processReturnCode) {
        System.out.println(message);
        System.exit(processReturnCode);
    }
}
