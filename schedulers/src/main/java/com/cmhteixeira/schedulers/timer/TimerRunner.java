package com.cmhteixeira.schedulers.timer;

import java.util.Timer;
import java.util.TimerTask;

public class TimerRunner {
  public static void main(String[] args) throws InterruptedException {
    Timer timer = new Timer("concurrency-deep-dives-timer-thread");

    TimerTask taskA = new QuickTask("A");
    TimerTask longTask = new BackupDB2(13, "B");

    timer.schedule(longTask, 3_000L);
    timer.scheduleAtFixedRate(taskA, 0L, 5_000L);
  }
}

// Normal
// 0 s - Start db
// 10 s - End db
// 12 s - Start db
// 22 s - End db
// 24 s - Start db
// 34 s - End db
// 36 s - Start db

// Fixed delay
// 0 s - Start db
// 10 s - End db
// 11 s - Start one-off
// 16 s - End one-off
// 16 s - Start db
// 26 s - End db
// 28 s - Start db
