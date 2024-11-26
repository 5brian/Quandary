package interpreter;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.TimeUnit;

public class HeapObject {
    private Object left;
    private Object right;
    private static long nextId = 1;
    private final long id;
    private final Lock lock = new ReentrantLock(true);
    private volatile Thread lockHolder = null;
    private static final long LOCK_TIMEOUT_MS = 50;

    public HeapObject(Object left, Object right) {
        this.left = left;
        this.right = right;
        this.id = nextId++;
    }

    public boolean tryAcquireLock() {
        try {
            if (lock.tryLock(LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                lockHolder = Thread.currentThread();
                return true;
            }
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public boolean releaseLock() {
        if (lockHolder == Thread.currentThread()) {
            try {
                lockHolder = null;
                lock.unlock();
                return true;
            } catch (IllegalMonitorStateException e) {
                return false;
            }
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