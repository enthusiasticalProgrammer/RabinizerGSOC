package rabinizer.exec;

public class Tuple<L, R> {

    private final L left;
    private final R right;
    private String strTuple = null;

    public Tuple(L l, R r) {
        left = l;
        right = r;
    }

    @Override
    public String toString() {
        if (strTuple == null) {
            strTuple = "<" + (left == null ? "null" : left.toString()) + ", "
                    + (right == null ? "null" : right.toString()) + ">";
        }
        return strTuple;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Tuple<?, ?>) {
            try {
                @SuppressWarnings("unchecked")
                Tuple<L, R> tup = (Tuple<L, R>) o;
                return left.equals(tup.left) && right.equals(tup.right);
            } catch (ClassCastException e) {
                return false;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int l = 0, r = 0;
        if (left != null) {
            l = left.hashCode();
        }
        if (right != null) {
            r = right.hashCode();
        }
        return 17 * l + 5 * r;
    }

    public L getLeft() {
        return left;
    }

    public R getRight() {
        return right;
    }
}
