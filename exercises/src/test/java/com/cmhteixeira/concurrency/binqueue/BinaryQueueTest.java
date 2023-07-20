package com.cmhteixeira.concurrency.binqueue;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cmhteixeira.MyPriorityBlockingQueue;
import java.util.Comparator;
import java.util.PriorityQueue;
import org.junit.jupiter.api.Test;

public class BinaryQueueTest {

  record TimerTask(int ttr) {}

  static class TimerTaskComparable implements Comparator<TimerTask> {

    @Override
    public int compare(TimerTask o1, TimerTask o2) {
      var res = o1.ttr - o2.ttr;
      if (res < 0) return -1;
      else if (res == 0) return 0;
      else return 1;
    }
  }

  @Test
  public void foo() {
    var fooo = new MyPriorityBlockingQueue<>(new TimerTaskComparable());
    fooo.insert(new TimerTask(21));
    fooo.insert(new TimerTask(13));
    fooo.insert(new TimerTask(16));
    fooo.insert(new TimerTask(32));
    fooo.insert(new TimerTask(65));
    assertEquals(fooo.findMin().ttr, 13);
  }

  @Test
  public void bar() {
    var fooo = new MyPriorityBlockingQueue<>(new TimerTaskComparable());
    int iter = 1000000;
    while (iter >= 10) {
      fooo.insert(new TimerTask(iter));
      --iter;
    }
    fooo.insert(new TimerTask(4));
    assertEquals(fooo.findMin().ttr, 4);
  }

  @Test
  public void qux() {
    var fooo = new MyPriorityBlockingQueue<>(new TimerTaskComparable());
    int iter = 1000000;
    while (iter >= 10) {
      fooo.insert(new TimerTask(iter));
      --iter;
    }
    fooo.insert(new TimerTask(4));
    assertEquals(fooo.findMin().ttr, 4);
  }

  @Test
  public void baz() {
    var queue = new MyPriorityBlockingQueue<>(new TimerTaskComparable());
    int iter = 20;
    while (iter >= 10) {
      queue.insert(new TimerTask(iter));
      --iter;
    }

    queue.insert(new TimerTask(4));
    queue.insert(new TimerTask(3));
    queue.insert(new TimerTask(2));
    queue.insert(new TimerTask(1));
    assertEquals(queue.popMin().ttr, 1);
    assertEquals(queue.popMin().ttr, 2);
    assertEquals(queue.popMin().ttr, 3);
    assertEquals(queue.popMin().ttr, 4);
    //        int randomWithMathRandom = (int) ((Math.random() * (max - min)) + min);
    //        assertEquals(fooo.popMin().ttr, 10);
  }

  @Test
  public void quux() {

    var queue = new MyPriorityBlockingQueue<>(new TimerTaskComparable());
    var prioQueue = new PriorityQueue<>(new TimerTaskComparable());
    int max = 56555;
    int min = 1;
    int maxIter = 1700000;
    int minIter = 1700000;
    int iter = (int) ((Math.random() * (maxIter - minIter)) + minIter);
    int iter2 = iter;
    System.out.printf("Number iterations: %d. Max Value: %d. Min Value: %d%n", iter, max, min);
    while (iter >= 1) {
      int randomWithMathRandom = (int) ((Math.random() * (max - min)) + min);
      var timerTask = new TimerTask(randomWithMathRandom);
      queue.insert(timerTask);
      prioQueue.add(timerTask);
      --iter;
    }
    //        var theirResult = prioQueue.poll().ttr;
    //        System.out.println(String.format("Min is: %d", theirResult));
    //        var myresult = queue.popMin().ttr;
    //        assertEquals(myresult, theirResult);
    int j = 1;
    while (j <= iter2) {
      var theirResult = prioQueue.poll().ttr;
      System.out.println(String.format("Min is: %d", theirResult));
      assertEquals(queue.popMin().ttr, theirResult);
      ++j;
    }
  }
}
