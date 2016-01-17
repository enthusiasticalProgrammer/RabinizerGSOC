package rabinizer.collections;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Collection;

public class TrivialTarjanStack<E> extends TarjanStack<E> {

    private static final long serialVersionUID = -4238745783435406609L;

    @Override
    public boolean contains(Object o) {
        return this.clone().contains(o);// because super.super is impossible
    }

    @Override
    public boolean isEmpty() {
        return this.clone().isEmpty();
    }

    @Override
    public TrivialTarjanStack<E> clone() {
        TrivialTarjanStack<E> stack = new TrivialTarjanStack<>();
        stack.stack = new ArrayDeque<>(this.stack);
        return stack;
    }

    @Override
    protected void rmElem(Object o) {
    }

    @Override
    protected void addElem(Object o) {
    }

    @Override
    protected void rmAllElems() {
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends E> c) {
        return stack.addAll(c);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return stack.containsAll(c);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return stack.removeAll(c);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        return stack.retainAll(c);
    }

}
