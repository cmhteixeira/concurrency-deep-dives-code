package com.cmhteixeira.concurrency.binqueue;

import java.util.Timer;
import java.util.TimerTask;

public class TempTest {
  Timer timer = new Timer();

  public void foo() {
    timer.schedule(
        new TimerTask() {
          @Override
          public void run() {}
        },
        5);
  }
}
