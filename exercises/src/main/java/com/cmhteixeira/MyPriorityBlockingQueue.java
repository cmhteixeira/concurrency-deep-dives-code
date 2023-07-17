package com.cmhteixeira;

import java.util.Comparator;

public final class MyPriorityBlockingQueue<E> {
    private final static int DEFAULT_CAPACITY = 1024;

    private final Comparator<? super E> comparator;

    private E[] queue; // zero based

    private int numberElem;

    public MyPriorityBlockingQueue(int initialCapacity, Comparator<? super E> comparator) {
        this.comparator = comparator;
        this.numberElem = 0;
        queue = (E[]) new Object[initialCapacity];
    }

    public MyPriorityBlockingQueue(Comparator<? super E> comparator) {
        this(DEFAULT_CAPACITY, comparator);
    }


    private void insertInternal(E elem) {
        ++numberElem;
        queue[numberElem - 1] = elem;
        perculateUp();
    }

    public void insert(E elem) {
        if (elem == null) throw new NullPointerException("No null values permitted.");
        if (numberElem == Integer.MAX_VALUE) throw new RuntimeException("Limit reached");
        var currentCapacity = queue.length;
        if (currentCapacity == numberElem) {
            int newCapacity = (((long) currentCapacity * 2) < (long) Integer.MAX_VALUE) ? currentCapacity * 2 : Integer.MAX_VALUE;
            E[] newQueue = (E[]) new Object[newCapacity];
            System.arraycopy(queue, 0, newQueue, 0, queue.length);
            queue = newQueue;
        }

        insertInternal(elem);
    }

    private void perculateUp() {
        int indexElem = numberElem - 1;

        while (indexElem > 0) {
            var indexParent = indexParent(indexElem);
            var valueCurrent = queue[indexElem];
            var valueParent = queue[indexParent];
            if (comparator.compare(valueCurrent, valueParent) <= 0) {
                queue[indexElem] = valueParent;
                queue[indexParent] = valueCurrent;
                indexElem = indexParent;
            } else break;
        }
    }

    private void perculateDown() {
        int indexElem = 0;

        while (indexElem < numberElem - 1) {
            var indexChildL = indexChildL(indexElem);
            var indexChildR = indexChildR(indexElem);
            if (indexChildL > numberElem - 1) break;

            int indexChosenChild;

            if (indexChildR > numberElem - 1) {
                indexChosenChild = indexChildL;
            } else {
                if (comparator.compare(queue[indexChildL], queue[indexChildR]) <= 0) indexChosenChild = indexChildL;
                else indexChosenChild = indexChildR;
            }

            var valueCurrent = queue[indexElem];
            var valueChosenChild = queue[indexChosenChild];

            if (comparator.compare(valueCurrent, valueChosenChild) <= 0) break;
            queue[indexChosenChild] = valueCurrent;
            queue[indexElem] = valueChosenChild;
            indexElem = indexChosenChild;
        }
    }

    public E findMin() {
        return queue[0];
    }

    public E popMin() {
        if (numberElem == 0) throw new RuntimeException("No elements");
        E elemToReturn = queue[0];
        queue[0] = queue[numberElem - 1];
        queue[numberElem - 1] = null;
        --numberElem;
        perculateDown();
        return elemToReturn;
    }

    private int indexChildL(int index) {
        return 2 * index;
    }

    private int indexChildR(int index) {
        return 2 * index + 1;
    }

    private int indexParent(int index) {
        return (int) Math.floor(index * 1.0 / 2);
    }

    /**
     * Method that calculates the floor of the log, base 2,
     * of 'x'. The calculation is performed in integer arithmetic,
     * therefore, it is exact.
     *
     * @param x The value to calculate log2 on.
     * @return floor(log ( x)/log(2)), calculated in an exact way.
     */
    public static int log2(int x) {
        int y, v;
        // No log of 0 or negative
        if (x <= 0) {
            throw new IllegalArgumentException("" + x + " <= 0");
        }
        // Calculate log2 (it's actually floor log2)
        v = x;
        y = -1;
        while (v > 0) {
            v >>= 1;
            y++;
        }
        return y;
    }
}
