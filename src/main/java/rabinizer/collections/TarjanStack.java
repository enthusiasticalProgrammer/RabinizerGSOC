package rabinizer.collections;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * it is designed to be a stack that can handle push, pop, peek, contains and
 * isEmpty in constant time under the assumption, that the hash function of the
 * elements is distributed reasonably well. However, as the TrivialTarjanStack,
 * some subclasses may take longer in theory, but they might be faster, because
 * we are using only rather small automata.
 * 
 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!Important!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
 * This class always assumes the Objects to be unique, i.e. no Object can be in
 * the stack twice
 * 
 */

public abstract class TarjanStack<E> implements Deque<E> {

    Deque<E> stack;

    public TarjanStack() {
        stack = new ArrayDeque<E>();
    }

    @Override
    public abstract boolean contains(Object o);

    @Override
    public abstract boolean isEmpty();

    @Override
    public abstract TarjanStack<E> clone();

    @Override
    public E pop() {
        E e = stack.pop();
        rmElem(e);
        return e;
    }

    @Override
    public void push(E e) {
        stack.push(e);
        addElem(e);
    }

    @Override
    public boolean add(E e) {
        addElem(e);
        return stack.add(e);
    }

    @Override
    public void addFirst(E e) {
        stack.addFirst(e);
        addElem(e);
    }

    @Override
    public void addLast(E e) {
        stack.addLast(e);
        addElem(e);
    }

    @Override
    public void clear() {
        rmAllElems();
        stack.clear();
    }

    @Override
    public boolean offer(E e) {
        addElem(e);
        return stack.offer(e);
    }

    @Override
    public boolean offerFirst(E e) {
        addElem(e);
        return stack.offerFirst(e);
    }

    @Override
    public boolean offerLast(E e) {
        addElem(e);
        return stack.offerLast(e);
    }

    @Override
    public E poll() {
        E e = stack.poll();
        rmElem(e);
        return e;
    }

    @Override
    public E pollFirst() {
        E e = stack.pollFirst();
        rmElem(e);
        return e;
    }

    @Override
    public E pollLast() {
        E e = stack.pollLast();
        rmElem(e);
        return e;
    }

    @Override
    public E remove() {
        E e = stack.remove();
        rmElem(e);
        return e;
    }

    @Override
    public boolean remove(Object o) {
        rmElem(o);
        return stack.remove(o);
    }

    @Override
    public E removeFirst() {
        E e = stack.removeFirst();
        rmElem(e);
        return e;
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        if (this.contains(o)) {
            this.remove(o);
            return true;
        }
        return false;
    }

    @Override
    public E removeLast() {
        E e = stack.removeLast();
        rmElem(e);
        return e;
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        return removeFirstOccurrence(o);
    }

    protected abstract void rmElem(Object o);

    protected abstract void addElem(Object o);

    protected abstract void rmAllElems();

    @Override
    public Iterator<E> descendingIterator() {
        return stack.descendingIterator();
    }

    @Override
    public E element() {
        return stack.element();
    }

    @Override
    public E getFirst() {
        return stack.getFirst();
    }

    @Override
    public E getLast() {
        return stack.getLast();
    }

    @Override
    public Iterator<E> iterator() {
        return stack.iterator();
    }

    @Override
    public E peek() {
        return stack.peek();
    }

    @Override
    public E peekFirst() {
        return stack.peekFirst();
    }

    @Override
    public E peekLast() {
        return stack.peekLast();
    }

    @Override
    public int size() {
        return stack.size();
    }

    @Override
    public Object[] toArray() {
        return stack.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return stack.toArray(a);
    }

}
