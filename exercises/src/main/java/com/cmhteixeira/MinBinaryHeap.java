package com.cmhteixeira;

import java.util.Comparator;
import java.util.NoSuchElementException;

public final class MinBinaryHeap<E> {
  private static final int DEFAULT_CAPACITY = 1024;

  private final Comparator<? super E> comparator;

  private E[] queue; // zero based

  private int numberElem;

  public MinBinaryHeap(int initialCapacity, Comparator<? super E> comparator) {
    this.comparator = comparator;
    this.numberElem = 0;
    queue = (E[]) new Object[initialCapacity];
  }

  public MinBinaryHeap(Comparator<? super E> comparator) {
    this(DEFAULT_CAPACITY, comparator);
  }

  private void insertInternal(E elem) {
    queue[++numberElem - 1] = elem;
    perculateUp();
  }

  public void insert(E elem) {
    if (elem == null) throw new NullPointerException("No null values permitted.");
    if (numberElem == Integer.MAX_VALUE) throw new RuntimeException("Limit reached");
    var currentCapacity = queue.length;
    if (currentCapacity == numberElem) {
      int newCapacity =
          (((long) currentCapacity * 2) < (long) Integer.MAX_VALUE)
              ? currentCapacity * 2
              : Integer.MAX_VALUE;
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
        if (comparator.compare(queue[indexChildL], queue[indexChildR]) <= 0)
          indexChosenChild = indexChildL;
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
    if (numberElem == 0) throw new NoSuchElementException("No elements");
    E elemToReturn = queue[0];
    queue[0] = queue[numberElem - 1];
    queue[--numberElem] = null;
    perculateDown();
    return elemToReturn;
  }

  private int indexChildL(int index) {
    return 2 * index + 1;
  }

  private int indexChildR(int index) {
    return 2 * index + 2;
  }

  private int indexParent(int index) {
    return (int) Math.ceil(index * 1.0 / 2) - 1;
  }
}
