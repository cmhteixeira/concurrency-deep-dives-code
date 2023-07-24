package com.cmhteixeira.concurrency.binqueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.cmhteixeira.MinBinaryHeap;
import java.util.Comparator;
import java.util.PriorityQueue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class MinBinaryQueueTest {
    @Test
    public void returnsMinWhenInsertedSorted() {
        var minHeap = new MinBinaryHeap<Integer>(Comparator.naturalOrder());
        var from = 1;
        var to = 10_000;
        for (int iter = from; iter <= to; ++iter) {
            minHeap.insert(iter);
        }
        assertEquals(minHeap.findMin(), from);
    }

    @Test
    public void returnsMinWhenInsertedInReverseSorted() {
        var minHeap = new MinBinaryHeap<Integer>(Comparator.naturalOrder());
        var from = 10_000;
        var to = 1;
        for (int iter = from; iter >= to; --iter) {
            minHeap.insert(iter);
        }
        assertEquals(minHeap.findMin(), to);
    }

    @Test
    public void insertNewMinIntoNewLevelPropagatesUp() {
        var minHeap = new MinBinaryHeap<Integer>(Comparator.naturalOrder());
        minHeap.insert(3);
        minHeap.insert(4);
        minHeap.insert(6);
        minHeap.insert(1);
        minHeap.insert(2);
        minHeap.insert(5);

        minHeap.insert(-1);
        assertEquals(minHeap.popMin(), -1);
    }

    @Test
    public void insertNewMinToCompleteLevel() {
        var minHeap = new MinBinaryHeap<Integer>(Comparator.naturalOrder());
        minHeap.insert(3);
        minHeap.insert(4);
        minHeap.insert(6);
        minHeap.insert(1);
        minHeap.insert(2);
        minHeap.insert(-1);

        assertEquals(minHeap.popMin(), -1);
    }

    @Test
    public void insertNewMaxToCompleteLevel() {
        var minHeap = new MinBinaryHeap<Integer>(Comparator.naturalOrder());
        minHeap.insert(3);
        minHeap.insert(4);
        minHeap.insert(6);
        minHeap.insert(1);
        minHeap.insert(2);
        minHeap.insert(7);

        assertEquals(minHeap.popMin(), 1);
    }

    @Test
    public void insertNewMaxIntoNewLevel() {
        var minHeap = new MinBinaryHeap<Integer>(Comparator.naturalOrder());
        minHeap.insert(3);
        minHeap.insert(4);
        minHeap.insert(6);
        minHeap.insert(1);
        minHeap.insert(2);
        minHeap.insert(5);

        minHeap.insert(7);
        assertEquals(minHeap.popMin(), 1);
    }

    @Test
    public void deleteFromFullLevel() {
        var minHeap = new MinBinaryHeap<Integer>(Comparator.naturalOrder());
        minHeap.insert(3);
        minHeap.insert(4);
        minHeap.insert(6);
        minHeap.insert(1);
        minHeap.insert(2);
        minHeap.insert(5);

        assertEquals(minHeap.popMin(), 1);
    }

    @Test
    @DisplayName("Nondeterministic test. The behaviour should be the same as java's PriorityQueue.")
    public void randomInputCompareJavaPriorityQueue() {
        var minQueue = new MinBinaryHeap<Integer>(Comparator.naturalOrder());
        var prioQueue = new PriorityQueue<Integer>(Comparator.naturalOrder());
        int maxIter = 100_000;
        int minIter = 1;
        int max = maxIter * 2;
        int min = 1;

        int iter = (int) ((Math.random() * (maxIter - minIter)) + minIter);
        System.out.printf("Number elements: %d", iter);
        for (int i = 1; i <= iter; ++i) {
            int randomWithMathRandom = (int) ((Math.random() * (max - min)) + min);
            minQueue.insert(randomWithMathRandom);
            prioQueue.add(randomWithMathRandom);
        }
        for (int i = 1; i <= iter; ++i) {
            assertEquals(minQueue.popMin(), prioQueue.poll());
        }
    }
}
