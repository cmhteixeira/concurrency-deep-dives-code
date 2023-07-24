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
    int iC = numberElem - 1; // index of child

    while (iC > 0) {
      var iP = indexParent(iC); // index of parent
      var valueC = queue[iC];
      var valueP = queue[iP];
      if (comparator.compare(valueC, valueP) >= 0) break;
      queue[iC] = valueP;
      queue[iP] = valueC;
      iC = iP;
    }
  }

  private void perculateDown() {
    int iP = 0; // index of parent

    while (indexChildL(iP) <= numberElem - 1) {
      var iL = indexChildL(iP);
      var iR = indexChildR(iP);
      int iC; // index of child
      if (iR > numberElem - 1) iC = iL;
      else {
        if (comparator.compare(queue[iL], queue[iR]) <= 0) iC = iL;
        else iC = iR;
      }

      var valueP = queue[iP]; // value of parent
      var valueC = queue[iC]; // value of child
      if (comparator.compare(valueP, valueC) <= 0) break;
      queue[iC] = valueP;
      queue[iP] = valueC;
      iP = iC;
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
