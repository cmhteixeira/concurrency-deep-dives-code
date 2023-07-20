package com.cmhteixeira.java;

import com.cmhteixeira.MyPriorityBlockingQueue;
import java.util.Comparator;
import java.util.Timer;

public class Runner {
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

  public static void main(String[] args) {
    new Timer()
    var fooo = new MyPriorityBlockingQueue<>(new TimerTaskComparable());
    fooo.insert(new TimerTask(13));
    fooo.insert(new TimerTask(21));
    fooo.insert(new TimerTask(16));
    fooo.insert(new TimerTask(32));
    fooo.insert(new TimerTask(65));
    System.out.println(fooo.findMin().ttr);
  }
}
