package rabinizer.collections;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class HashTarjanStack<E> extends TarjanStack<E> {

    Set<E> elementsInTheStack;

    public HashTarjanStack() {
        super();
        elementsInTheStack = new HashSet<>();
    }

    @Override
    public boolean contains(Object o) {
        return elementsInTheStack.contains(o);
    }

    @Override
    public boolean isEmpty() {
        return elementsInTheStack.isEmpty();
    }

    @Override
    public HashTarjanStack<E> clone() {
        HashTarjanStack<E> stack = new HashTarjanStack<>();
        stack.stack = new ArrayDeque<>(this.stack);
        stack.elementsInTheStack = new HashSet<>(this.elementsInTheStack);
        return stack;

    }

    @Override
    protected void rmElem(Object o) {
        elementsInTheStack.remove(o);
    }

    @Override
    protected void addElem(Object o) {
        elementsInTheStack.add((E) o);
    }

    @Override
    protected void rmAllElems() {
        elementsInTheStack.clear();
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {

        // stream with anyMatch not possible due to side effects
        boolean result = false;
        for (E e : c) {
            if (!elementsInTheStack.contains(e)) {
                result = true;
                this.push(e);
            }
        }
        return result;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return c.stream().allMatch(x -> elementsInTheStack.contains(x));
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean result = false;
        for (Object o : c) {
            result = result || remove(o);
        }
        return result;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        stack.retainAll(c);
        return elementsInTheStack.retainAll(c);
    }

}
