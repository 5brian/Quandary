package interpreter;

public class HeapObject {
    private Object left;
    private Object right;
    private static long nextId = 1;
    private final long id;
    private volatile Thread lockHolder = null;

    public HeapObject(Object left, Object right) {
        this.left = left;
        this.right = right;
        this.id = nextId++;
    }

    public synchronized boolean tryAcquireLock() {
        if (lockHolder == null) {
            lockHolder = Thread.currentThread();
            return true;
        }
        return false;
    }

    public synchronized boolean releaseLock() {
        if (lockHolder == Thread.currentThread()) {
            lockHolder = null;
            return true;
        }
        return false;
    }

    public Object getLeft() {
        return left;
    }

    public Object getRight() {
        return right;
    }

    public void setLeft(Object value) {
        this.left = value;
    }

    public void setRight(Object value) {
        this.right = value;
    }

    @Override
    public String toString() {
        String leftStr = left == null ? "nil" : left.toString();
        String rightStr = right == null ? "nil" : right.toString();
        return "(" + leftStr + " . " + rightStr + ")";
    }
}