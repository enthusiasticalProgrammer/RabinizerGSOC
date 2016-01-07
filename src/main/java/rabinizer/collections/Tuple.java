package rabinizer.collections;

import java.util.Objects;

public class Tuple<L, R> {

    public final L left;
    public final R right;

    public Tuple(L l, R r) {
        Objects.nonNull(l);
        Objects.nonNull(r);
        left = l;
        right = r;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tuple<?, ?> tuple = (Tuple<?, ?>) o;
        return Objects.equals(left, tuple.left) &&
                Objects.equals(right, tuple.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }

    @Override
    public String toString() {
        return "<" + left + ", " + right + '>';
    }
}
