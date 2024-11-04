package interpreter;

public class HeapObject {
    private Object left;
    private Object right;
    private static long nextId = 1;
    private final long id;

    public HeapObject(Object left, Object right) {
        this.left = left;
        this.right = right;
        this.id = nextId++;
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