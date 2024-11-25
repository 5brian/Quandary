package interpreter;

import java.io.*;
import java.util.*;
import parser.ParserWrapper;
import ast.*;

public class Interpreter {
    public static final int EXIT_SUCCESS = 0;
    public static final int EXIT_PARSING_ERROR = 1;
    public static final int EXIT_STATIC_CHECKING_ERROR = 2;
    public static final int EXIT_DYNAMIC_TYPE_ERROR = 3;
    public static final int EXIT_NIL_REF_ERROR = 4;
    public static final int EXIT_QUANDARY_HEAP_OUT_OF_MEMORY_ERROR = 5;

    static private Interpreter interpreter;
    private final ThreadLocal<Stack<Map<String, Object>>> threadLocalEnvStack = new ThreadLocal<>();

    public static Interpreter getInterpreter() {
        return interpreter;
    }

    private static class EvalResult {
        Object value;
        RuntimeException error;
    }

    private static class EvalThread extends Thread {
        private final Expr expr;
        private final Map<String, Object> env;
        private final EvalResult result;
        private final Stack<Map<String, Object>> threadEnvStack;

        public EvalThread(Expr expr, Map<String, Object> env, EvalResult result,
                Stack<Map<String, Object>> parentEnvStack) {
            this.expr = expr;
            this.env = env;
            this.result = result;
            this.threadEnvStack = new Stack<>();
            for (Map<String, Object> map : parentEnvStack) {
                this.threadEnvStack.push(new HashMap<>(map));
            }
        }

        @Override
        public void run() {
            try {
                Interpreter interpreter = Interpreter.getInterpreter();
                interpreter.pushThreadLocalEnvStack(threadEnvStack);
                result.value = interpreter.evaluate(expr, env);
            } catch (RuntimeException e) {
                result.error = e;
            } finally {
                Interpreter.getInterpreter().popThreadLocalEnvStack();
            }
        }
    }

    private void pushThreadLocalEnvStack(Stack<Map<String, Object>> stack) {
        threadLocalEnvStack.set(stack);
    }

    private void popThreadLocalEnvStack() {
        threadLocalEnvStack.remove();
    }

    private Stack<Map<String, Object>> getCurrentEnvStack() {
        Stack<Map<String, Object>> stack = threadLocalEnvStack.get();
        return stack != null ? stack : envStack;
    }

    public static void main(String[] args) {
        String gcType = "NoGC";
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
        Object returnValue = interpreter.executeRoot(astRoot, quandaryArg);
        System.out.println("Interpreter returned " + formatValue(returnValue));
    }

    private static String formatValue(Object value) {
        if (value == null) {
            return "nil";
        } else if (value instanceof HeapObject) {
            return value.toString();
        } else {
            return value.toString();
        }
    }

    final Program astRoot;
    final Random random;
    private final Map<String, FuncDef> functions = new HashMap<>();
    private final Stack<Map<String, Object>> envStack = new Stack<>();
    private final Map<String, Boolean> mutableVars = new HashMap<>();

    private Interpreter(Program astRoot) {
        this.astRoot = astRoot;
        this.random = new Random();
        for (FuncDef funcDef : astRoot.getFuncDefList()) {
            functions.put(funcDef.getName(), funcDef);
        }
    }

    void initMemoryManager(String gcType, long heapBytes) {
        if (gcType.equals("Explicit") || gcType.equals("MarkSweep")) {
            throw new RuntimeException(gcType + " not implemented");
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
            VarDecl param = params.get(i);
            localVars.put(param.getName(), args.get(i));
            if (param.isMutable()) {
                mutableVars.put(param.getName(), true);
            }
        }

        Stack<Map<String, Object>> currentStack = getCurrentEnvStack();
        currentStack.push(localVars);
        Object result = null;

        List<Stmt> body = funcDef.getBody();
        if (body.isEmpty()) {
            currentStack.pop();
            throw new RuntimeException("Function must end with a return statement: " + funcDef.getName());
        }

        for (Stmt stmt : body) {
            result = executeStatement(stmt);
            if (result instanceof ReturnValue) {
                currentStack.pop();
                return ((ReturnValue) result).getValue();
            }
        }

        currentStack.pop();
        throw new RuntimeException("Function must end with a return statement: " + funcDef.getName());
    }

    Object executeStatement(Stmt stmt) {
        Stack<Map<String, Object>> currentStack = getCurrentEnvStack();
        if (stmt instanceof VarDecl) {
            VarDecl varDecl = (VarDecl) stmt;
            Object value = evaluate(varDecl.getInitExpr(), currentStack.peek());
            currentStack.peek().put(varDecl.getName(), value);
            if (varDecl.isMutable()) {
                mutableVars.put(varDecl.getName(), true);
            }
        } else if (stmt instanceof AssignStmt) {
            AssignStmt assignStmt = (AssignStmt) stmt;
            String name = assignStmt.getName();
            if (!isMutable(name)) {
                throw new RuntimeException("Cannot assign to immutable variable: " + name);
            }
            Object value = evaluate(assignStmt.getExpr(), currentStack.peek());
            boolean found = false;
            for (int i = currentStack.size() - 1; i >= 0; i--) {
                if (currentStack.get(i).containsKey(name)) {
                    currentStack.get(i).put(name, value);
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new RuntimeException("Undefined variable: " + name);
            }
        } else if (stmt instanceof PrintStmt) {
            PrintStmt printStmt = (PrintStmt) stmt;
            System.out.println(evaluate(printStmt.getExpr(), currentStack.peek()));
        } else if (stmt instanceof IfStmt) {
            IfStmt ifStmt = (IfStmt) stmt;
            if ((Boolean) evaluate(ifStmt.getCondition(), currentStack.peek())) {
                return executeStatement(ifStmt.getThenStmt());
            } else if (ifStmt.getElseStmt() != null) {
                return executeStatement(ifStmt.getElseStmt());
            }
        } else if (stmt instanceof WhileStmt) {
            WhileStmt whileStmt = (WhileStmt) stmt;
            while ((Boolean) evaluate(whileStmt.getCondition(), currentStack.peek())) {
                Object result = executeStatement(whileStmt.getBody());
                if (result instanceof ReturnValue) {
                    return result;
                }
            }
        } else if (stmt instanceof BlockStmt) {
            BlockStmt blockStmt = (BlockStmt) stmt;
            currentStack.push(new HashMap<>());
            List<Stmt> statements = blockStmt.getStatements();
            if (statements.isEmpty()) {
                currentStack.pop();
                return null;
            }
            for (Stmt s : statements) {
                Object result = executeStatement(s);
                if (result instanceof ReturnValue) {
                    currentStack.pop();
                    return result;
                }
            }
            currentStack.pop();
        } else if (stmt instanceof ReturnStmt) {
            ReturnStmt returnStmt = (ReturnStmt) stmt;
            return new ReturnValue(evaluate(returnStmt.getExpr(), currentStack.peek()));
        } else if (stmt instanceof CallStmt) {
            CallStmt callStmt = (CallStmt) stmt;
            List<Object> argValues = new ArrayList<>();
            for (Expr arg : callStmt.getArgs()) {
                argValues.add(evaluate(arg, currentStack.peek()));
            }
            executeBuiltinOrUserFunction(callStmt.getName(), argValues);
        }
        return null;
    }

    Object evaluate(Expr expr, Map<String, Object> env) {
        Stack<Map<String, Object>> currentStack = getCurrentEnvStack();
        if (expr instanceof ConstExpr) {
            return ((ConstExpr) expr).getValue();
        } else if (expr instanceof NilExpr) {
            return null;
        } else if (expr instanceof VarExpr) {
            String name = ((VarExpr) expr).getName();
            if (env.containsKey(name)) {
                return env.get(name);
            }
            for (int i = currentStack.size() - 1; i >= 0; i--) {
                if (currentStack.get(i).containsKey(name)) {
                    return currentStack.get(i).get(name);
                }
            }
            throw new RuntimeException("Undefined variable: " + name);
        } else if (expr instanceof TypeCastExpr) {
            TypeCastExpr typeCastExpr = (TypeCastExpr) expr;
            Object value = evaluate(typeCastExpr.getExpr(), env);
            if (typeCastExpr.getType() == Type.REF) {
                if (value != null && !(value instanceof HeapObject)) {
                    fatalError("Cannot cast non-reference to Ref", EXIT_DYNAMIC_TYPE_ERROR);
                }
            }
            return value;
        } else if (expr instanceof DotExpr) {
            DotExpr dotExpr = (DotExpr) expr;
            Object left = evaluate(dotExpr.getLeft(), env);
            Object right = evaluate(dotExpr.getRight(), env);
            return new HeapObject(left, right);
        } else if (expr instanceof BinaryExpr) {
            BinaryExpr binaryExpr = (BinaryExpr) expr;
            Object left = evaluate(binaryExpr.getLeftExpr(), env);
            Object right = evaluate(binaryExpr.getRightExpr(), env);

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
                    if (left == null)
                        return right == null;
                    if (right == null)
                        return false;
                    if (left instanceof Long && right instanceof Long) {
                        return ((Long) left).longValue() == ((Long) right).longValue();
                    }
                    return left.equals(right);
                case BinaryExpr.NEQ:
                    if (left == null)
                        return right != null;
                    if (right == null)
                        return true;
                    if (left instanceof Long && right instanceof Long) {
                        return ((Long) left).longValue() != ((Long) right).longValue();
                    }
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
            Object operand = evaluate(unaryExpr.getExpr(), env);
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
            List<Object> argValues = new ArrayList<>();
            for (Expr arg : callExpr.getArguments()) {
                argValues.add(evaluate(arg, env));
            }
            return executeBuiltinOrUserFunction(callExpr.getFuncName(), argValues);
        } else if (expr instanceof ConcurrentExpr) {
            return evaluateConcurrent((ConcurrentExpr) expr, env);
        }
        throw new RuntimeException("Unknown expression type");
    }

    private Object evaluateConcurrent(ConcurrentExpr expr, Map<String, Object> env) {
        EvalResult leftResult = new EvalResult();
        EvalResult rightResult = new EvalResult();

        Thread leftThread = new EvalThread(expr.getLeft(), new HashMap<>(env), leftResult, getCurrentEnvStack());
        Thread rightThread = new EvalThread(expr.getRight(), new HashMap<>(env), rightResult, getCurrentEnvStack());

        leftThread.start();
        rightThread.start();

        try {
            leftThread.join();
            rightThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException("Thread interrupted");
        }

        if (leftResult.error != null)
            throw leftResult.error;
        if (rightResult.error != null)
            throw rightResult.error;

        switch (expr.getOperator()) {
            case BinaryExpr.PLUS:
                return (Long) leftResult.value + (Long) rightResult.value;
            case BinaryExpr.MINUS:
                return (Long) leftResult.value - (Long) rightResult.value;
            case BinaryExpr.TIMES:
                return (Long) leftResult.value * (Long) rightResult.value;
            case BinaryExpr.DOT:
                return new HeapObject(leftResult.value, rightResult.value);
            default:
                throw new RuntimeException("Invalid operation in concurrent expression");
        }
    }

    private Object executeBuiltinOrUserFunction(String funcName, List<Object> args) {
        switch (funcName) {
            case "randomInt":
                if (args.size() != 1 || !(args.get(0) instanceof Long)) {
                    throw new RuntimeException("randomInt expects one integer argument");
                }
                return (long) random.nextInt(((Long) args.get(0)).intValue());
            case "left":
                if (args.size() != 1 || !(args.get(0) instanceof HeapObject)) {
                    fatalError("left() requires a Ref argument", EXIT_DYNAMIC_TYPE_ERROR);
                }
                if (args.get(0) == null) {
                    fatalError("Nil dereference in left()", EXIT_NIL_REF_ERROR);
                }
                return ((HeapObject) args.get(0)).getLeft();
            case "right":
                if (args.size() != 1 || !(args.get(0) instanceof HeapObject)) {
                    fatalError("right() requires a Ref argument", EXIT_DYNAMIC_TYPE_ERROR);
                }
                if (args.get(0) == null) {
                    fatalError("Nil dereference in right()", EXIT_NIL_REF_ERROR);
                }
                return ((HeapObject) args.get(0)).getRight();
            case "setLeft":
                if (args.size() != 2 || !(args.get(0) instanceof HeapObject)) {
                    fatalError("setLeft() requires a Ref and Q argument", EXIT_DYNAMIC_TYPE_ERROR);
                }
                if (args.get(0) == null) {
                    fatalError("Nil dereference in setLeft()", EXIT_NIL_REF_ERROR);
                }
                ((HeapObject) args.get(0)).setLeft(args.get(1));
                return 1L;
            case "setRight":
                if (args.size() != 2 || !(args.get(0) instanceof HeapObject)) {
                    fatalError("setRight() requires a Ref and Q argument", EXIT_DYNAMIC_TYPE_ERROR);
                }
                if (args.get(0) == null) {
                    fatalError("Nil dereference in setRight()", EXIT_NIL_REF_ERROR);
                }
                ((HeapObject) args.get(0)).setRight(args.get(1));
                return 1L;
            case "isAtom":
                if (args.size() != 1) {
                    fatalError("isAtom() requires one Q argument", EXIT_DYNAMIC_TYPE_ERROR);
                }
                return (args.get(0) == null || args.get(0) instanceof Long) ? 1L : 0L;
            case "isNil":
                if (args.size() != 1) {
                    fatalError("isNil() requires one Q argument", EXIT_DYNAMIC_TYPE_ERROR);
                }
                return args.get(0) == null ? 1L : 0L;
            case "acq":
                if (args.size() != 1 || !(args.get(0) instanceof HeapObject)) {
                    fatalError("acq() requires a Ref argument", EXIT_DYNAMIC_TYPE_ERROR);
                }
                if (args.get(0) == null) {
                    fatalError("Nil dereference in acq()", EXIT_NIL_REF_ERROR);
                }
                return ((HeapObject) args.get(0)).tryAcquireLock() ? 1L : 0L;
            case "rel":
                if (args.size() != 1 || !(args.get(0) instanceof HeapObject)) {
                    fatalError("rel() requires a Ref argument", EXIT_DYNAMIC_TYPE_ERROR);
                }
                if (args.get(0) == null) {
                    fatalError("Nil dereference in rel()", EXIT_NIL_REF_ERROR);
                }
                return ((HeapObject) args.get(0)).releaseLock() ? 1L : 0L;
            default:
                FuncDef funcDef = functions.get(funcName);
                if (funcDef == null) {
                    throw new RuntimeException("Undefined function: " + funcName);
                }
                return executeFunction(funcDef, args);
        }
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

    private boolean isMutable(String varName) {
        return mutableVars.containsKey(varName);
    }
}