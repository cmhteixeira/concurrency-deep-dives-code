package com.cmhteixeira;

import java.util.Comparator;

public final class MyPriorityBlockingQueue<E> {
    //PriorityQueue
    private final static int DEFAULT_CAPACITY = 1024;

    private final Comparator<? super E> comparator;
    private int currentHeight = 0;
    private int lastElemPos = 0;

    private E[] queue; // zero based

    public MyPriorityBlockingQueue(int initialCapacity, Comparator<? super E> comparator) {
        this.comparator = comparator;
        queue = (E[]) new Object[initialCapacity];
    }

    public MyPriorityBlockingQueue(Comparator<? super E> comparator) {
        this(DEFAULT_CAPACITY, comparator);
    }


    private void insertInternal(E elem) {
        if (numberElems() == 0) {
            lastElemPos = 1;
        } else {
            int maximumElemsCurrentLevel = 1 << currentHeight;
            int accElemsLevelAbove = maximumElemsCurrentLevel - 1;
            int indexCurrentLevel = numberElems() - accElemsLevelAbove;
            if (indexCurrentLevel == maximumElemsCurrentLevel) {
                lastElemPos = 1;
                ++currentHeight;
            } else {
                ++lastElemPos;
            }
        }
        queue[numberElems() - 1] = elem;
        perculateUp();
    }

    public void insert(E elem) {
        if (elem == null) throw new NullPointerException("No null values permitted.");
        var numElems = numberElems();
        if (numElems == Integer.MAX_VALUE) throw new RuntimeException("Limit reached");
        var currentCapacity = queue.length;
        if (currentCapacity == numElems) {
            int newCapacity = (((long) currentCapacity * 2) < (long) Integer.MAX_VALUE) ? currentCapacity * 2 : Integer.MAX_VALUE;
            E[] newQueue = (E[]) new Object[newCapacity];
            System.arraycopy(queue, 0, newQueue, 0, queue.length);
            queue = newQueue;
        }

        insertInternal(elem);
    }

    private void perculateUp() {
        int height = currentHeight;
        int pos = lastElemPos;

        while (height > 0) {
            var heightParent = height - 1;
            var posParent = (int) Math.ceil((double) pos / 2);
            var indexParent = arrayIndex(heightParent, posParent);
            var indexCurrent = arrayIndex(height, pos);
            var valueCurrent = queue[indexCurrent];
            var valueParent = queue[indexParent];
            if (comparator.compare(valueCurrent, valueParent) <= 0) {
                queue[indexCurrent] = valueParent;
                queue[indexParent] = valueCurrent;
                height = heightParent;
                pos = posParent;
            } else break;
        }
    }

    private void perculateDown() {
        int height = 0;
        int pos = 1;
        while (height < currentHeight) {
            var currentLastElemIndex = arrayIndex(currentHeight, lastElemPos);
            var heightChildren = height + 1;
            var posChildL = 2 * (pos - 1) + 1;
            var posChildR = posChildL + 1;
            var indexCurrent = arrayIndex(height, pos);
            var valueCurrent = queue[indexCurrent];
            var indexChildL = arrayIndex(heightChildren, posChildL);
            if (indexChildL > currentLastElemIndex) break;
            var indexChildR = indexChildL + 1;
            var valueChildL = queue[indexChildL];
            var valueChildR = queue[indexChildR];
            var leftOrRight = indexChildR <= currentLastElemIndex ? comparator.compare(valueChildL, valueChildR) : -1;
            if (leftOrRight <= 0) {
                var res = comparator.compare(valueCurrent, valueChildL);
                if (res <= 0) break;
                queue[indexChildL] = valueCurrent;
                queue[indexCurrent] = valueChildL;
                height = heightChildren;
                pos = posChildL;
            } else {
                var res = comparator.compare(valueCurrent, valueChildR);
                if (res <= 0) break;
                queue[indexChildR] = valueCurrent;
                queue[indexCurrent] = valueChildR;
                height = heightChildren;
                pos = posChildR;
            }
        }
    }

    private static int arrayIndex(int height, int pos) {
        return (1 << height) - 1 + pos - 1;
    }

    private int numberElems() {
        if (currentHeight == 0 && lastElemPos == 0) return 0;
        else return (1 << currentHeight) - 1 + lastElemPos;
    }


    public E findMin() {
        return queue[0];
    }

    public E popMin() {
        if (lastElemPos == 0) throw new RuntimeException("No elements");
        E elemToReturn = queue[0];
        int lastElemIndex = arrayIndex(currentHeight, lastElemPos);
        queue[0] = queue[lastElemIndex];
        queue[lastElemIndex] = null;
        if (lastElemPos == 1) {
            if (currentHeight == 0) {
                lastElemPos = 0;
            } else {
                --currentHeight;
                lastElemPos = (1 << currentHeight);
            }
        } else {
            --lastElemPos;
        }

        perculateDown();
        return elemToReturn;
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
