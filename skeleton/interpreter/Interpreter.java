package interpreter;

import java.io.*;
import java.util.*;

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
        interpreter = new Interpreter(astRoot);
        interpreter.initMemoryManager(gcType, heapBytes);
        String returnValueAsString = interpreter.executeRoot(astRoot, quandaryArg).toString();
        System.out.println("Interpreter returned " + returnValueAsString);
    }

    final Program astRoot;
    final Random random;
    private final Map<String, FuncDef> functions = new HashMap<>();
    private final Stack<Map<String, Object>> envStack = new Stack<>();

    private Interpreter(Program astRoot) {
        this.astRoot = astRoot;
        this.random = new Random();
        for (FuncDef funcDef : astRoot.getFuncDefList()) {
            functions.put(funcDef.getName(), funcDef);
        }
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
        FuncDef mainFunc = functions.get("main");
        if (mainFunc == null) {
            throw new RuntimeException("Main function not found");
        }
        return executeFunction(mainFunc, Collections.singletonList(arg));
    }

    private Object executeFunction(FuncDef funcDef, List<Object> args) {
        Map<String, Object> localVars = new HashMap<>();

        List<VarDecl> params = funcDef.getParams();
        if (params.size() != args.size()) {
            throw new RuntimeException("Incorrect number of arguments for function: " + funcDef.getName());
        }

        for (int i = 0; i < params.size(); i++) {
            localVars.put(params.get(i).getName(), args.get(i));
        }

        envStack.push(localVars);

        Object result = null;
        for (Stmt stmt : funcDef.getBody()) {
            result = executeStatement(stmt);
            if (result instanceof ReturnValue) {
                envStack.pop();
                return ((ReturnValue) result).getValue();
            }
        }

        envStack.pop();
        throw new RuntimeException("Function must end with a return statement: " + funcDef.getName());
    }

    Object executeStatement(Stmt stmt) {
        if (stmt instanceof VarDecl) {
            VarDecl varDecl = (VarDecl) stmt;
            envStack.peek().put(varDecl.getName(), evaluate(varDecl.getInitExpr()));
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
            envStack.push(new HashMap<>());
            for (Stmt s : blockStmt.getStatements()) {
                Object result = executeStatement(s);
                if (result instanceof ReturnValue) {
                    envStack.pop();
                    return result;
                }
            }
            envStack.pop();
        } else if (stmt instanceof CallStmt) {
            CallStmt callStmt = (CallStmt) stmt;
            executeCall(callStmt.getName(), callStmt.getArgs());
        }
        return null;
    }

    Object evaluate(Expr expr) {
        if (expr instanceof ConstExpr) {
            return ((ConstExpr) expr).getValue();
        } else if (expr instanceof VarExpr) {
            String name = ((VarExpr) expr).getName();
            for (int i = envStack.size() - 1; i >= 0; i--) {
                if (envStack.get(i).containsKey(name)) {
                    return envStack.get(i).get(name);
                }
            }
            throw new RuntimeException("Undefined variable: " + name);
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
        } else if (expr instanceof CallExpr) {
            CallExpr callExpr = (CallExpr) expr;
            return executeCall(callExpr.getFuncName(), callExpr.getArguments());
        }
        throw new RuntimeException("Unknown expression type");
    }

    private Object executeCall(String funcName, List<Expr> args) {
        List<Object> evaluatedArgs = new ArrayList<>();
        for (Expr arg : args) {
            evaluatedArgs.add(evaluate(arg));
        }

        if (funcName.equals("randomInt")) {
            if (evaluatedArgs.size() != 1 || !(evaluatedArgs.get(0) instanceof Long)) {
                throw new RuntimeException("randomInt expects one integer argument");
            }
            return (long) random.nextInt(((Long) evaluatedArgs.get(0)).intValue());
        }

        FuncDef funcDef = functions.get(funcName);
        if (funcDef == null) {
            throw new RuntimeException("Undefined function: " + funcName);
        }
        return executeFunction(funcDef, evaluatedArgs);
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