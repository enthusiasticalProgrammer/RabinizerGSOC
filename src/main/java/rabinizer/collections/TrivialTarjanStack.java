package rabinizer.collections;

import java.util.ArrayDeque;
import java.util.Collection;

public class TrivialTarjanStack<E> extends TarjanStack<E> {

    private static final long serialVersionUID = -4238745783435406609L;

    public TrivialTarjanStack() {
        super();
    }

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
        stack.stack = new ArrayDeque<E>(this.stack);
        return stack;
    }

    @Override
    protected void rmElem(Object o) {
        return;
    }

    @Override
    protected void addElem(Object o) {
        return;
    }

    @Override
    protected void rmAllElems() {
        return;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return stack.addAll(c);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return stack.containsAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return stack.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return stack.retainAll(c);
    }

}
